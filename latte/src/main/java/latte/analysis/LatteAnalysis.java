package latte.analysis;

import java.util.*;
import latte.api.Diagnostic;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Ownership analysis expressed as immutable state transitions over Spoon's Java AST. */
public final class LatteAnalysis {
  private final List<Diagnostic> diagnostics = new ArrayList<>();
  private Map<String, Permission> fieldPermissions = Map.of();
  private ReturnAliases returnAliases;
  private final Map<CtStatement, Set<String>> futureUses = new IdentityHashMap<>();
  private final Set<String> finalizableObjects = new HashSet<>();
  private Set<String> annotationNames = Set.of();

  private record Closure(CtExpression<?> expression, Scope scope) {}

  private final Map<String, Closure> closures = new HashMap<>();
  private final Set<CtExpression<?>> activeClosures =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private final Deque<EvaluationContext> evaluations = new ArrayDeque<>();
  private final Set<CtConstructor<?>> activeConstructors =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /** Call-specific facts stay separate from the declaration's published AST facts. */
  private record EvaluationContext(
      String site,
      Map<CtElement, LatteFacts> facts,
      Map<String, String> transfers,
      Map<CtElement, ClosureInvocation> invocations,
      Set<String> mutations) {
    EvaluationContext(String site) {
      this(
          site, new IdentityHashMap<>(), new HashMap<>(), new IdentityHashMap<>(), new HashSet<>());
    }
  }

  private record Evaluation(FlowState state, Reference value) {}

  private record Scope(CtExecutable<?> executable, String receiver) {}

  private enum Exit {
    NORMAL,
    RETURN,
    THROW,
    BREAK,
    CONTINUE
  }

  private record Flow(FlowState state, Exit exit) {}

  public List<Diagnostic> analyze(CtModel model) {
    diagnostics.clear();
    var external = ExternalSpecifications.collect(model);
    diagnostics.addAll(external.diagnostics());
    evaluations.clear();
    activeConstructors.clear();
    closures.clear();
    activeClosures.clear();
    futureUses.clear();
    finalizableObjects.clear();
    annotationNames = LocalObservability.annotationNames(model);
    model
        .getElements(new TypeFilter<>(CtStatement.class))
        .forEach(statement -> statement.putMetadata(OwnershipRelease.KEY, null));
    model
        .getElements(new TypeFilter<>(CtExpression.class))
        .forEach(
            expression -> {
              expression.putMetadata(ClosureFacts.KEY, null);
              expression.putMetadata(ClosureInvocation.KEY, null);
            });
    returnAliases = ReturnAliases.collect(model);
    MutationEffects.collect(model, returnAliases);
    fieldPermissions =
        model.getElements(new TypeFilter<>(CtField.class)).stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    f -> f.getReference().getQualifiedName(),
                    Annotations::permission,
                    (a, b) -> a));
    var declaredFields = new HashMap<>(fieldPermissions);
    external
        .specifications()
        .forEach(
            (owner, specification) ->
                specification
                    .fields()
                    .forEach(
                        (name, field) ->
                            declaredFields.put(owner + "#" + name, Annotations.permission(field))));
    fieldPermissions = Map.copyOf(declaredFields);
    model.getElements(new TypeFilter<>(CtExecutable.class)).stream()
        .filter(e -> e instanceof CtMethod<?> || e instanceof CtConstructor<?>)
        .filter(e -> !ExternalSpecifications.isLibrary(e))
        .forEach(this::executable);
    model
        .getElements(new TypeFilter<>(CtField.class))
        .forEach(
            field -> {
              var value =
                  scalar(field.getType())
                      ? Reference.scalarValue()
                      : Reference.object(Annotations.key(field));
              var state = FlowState.empty().allocate(value, Annotations.permission(field));
              new LatteFacts(state, state, value).attach(field);
            });
    return diagnostics.stream().distinct().toList();
  }

  private void executable(CtExecutable<?> executable) {
    var scope = new Scope(executable, "this:" + Annotations.key(executable));
    var receiver = Reference.object(scope.receiver());
    var initial =
        FlowState.empty()
            .withFieldPermissions(fieldPermissions)
            .allocate(receiver, Permission.BORROWED)
            .bind("this", receiver);
    for (var parameter : executable.getParameters()) {
      var value =
          scalar(parameter.getType())
              ? Reference.scalarValue()
              : Reference.object(Annotations.key(parameter));
      var after =
          initial
              .allocate(value, Annotations.permission(parameter))
              .bind(Annotations.key(parameter), value);
      new LatteFacts(initial, after, value).attach(parameter);
      if (mayFinalize(parameter.getType())) finalizableObjects.addAll(value.objects());
      initial = after;
    }
    var after =
        ExternalSpecifications.isSpecification(executable)
            ? initial
            : executable instanceof CtConstructor<?> constructor
                ? constructorBody(
                    constructor, defaults(constructor.getDeclaringType(), receiver, initial), scope)
                : statement(executable.getBody(), initial, scope).state();
    new LatteFacts(initial, after, receiver).attach(executable);
  }

  private Optional<LatteFacts> facts(CtElement element) {
    return evaluations.isEmpty()
        ? LatteFacts.on(element)
        : Optional.ofNullable(evaluations.peek().facts().get(element));
  }

  private void remember(CtElement element, LatteFacts facts) {
    if (evaluations.isEmpty()) facts.attach(element);
    else evaluations.peek().facts().put(element, facts);
  }

  private String allocation(String kind, CtElement site) {
    return kind
        + Annotations.key(site)
        + (evaluations.isEmpty() ? "" : ":in:" + evaluations.peek().site());
  }

  private FlowState defaults(CtType<?> type, Reference receiver, FlowState state) {
    for (var reference : type.getAllFields()) {
      var field = reference.getDeclaration();
      if (field != null && !field.isStatic())
        state =
            state.write(
                receiver,
                reference.getQualifiedName(),
                scalar(field.getType()) ? Reference.scalarValue() : Reference.nullValue());
    }
    return state;
  }

  private FlowState initializers(CtType<?> type, FlowState state, Scope scope) {
    var receiver = Reference.object(scope.receiver());
    for (var member : type.getTypeMembers()) {
      if (member instanceof CtField<?> field
          && !field.isStatic()
          && field.getDefaultExpression() != null) {
        var evaluated = expression(field.getDefaultExpression(), state, scope);
        state =
            transfer(evaluated.value(), Annotations.permission(field), evaluated.state(), field)
                .write(receiver, field.getReference().getQualifiedName(), evaluated.value());
      } else if (member instanceof CtAnonymousExecutable initializer && !initializer.isStatic()) {
        state = statement(initializer.getBody(), state, scope).state();
      }
    }
    return state;
  }

  private FlowState constructorBody(CtConstructor<?> constructor, FlowState state, Scope scope) {
    if (constructor.getBody() == null) return state;
    if (!activeConstructors.add(constructor)) {
      problem("Recursive constructor ownership requires a finite heap summary", constructor);
      return state;
    }
    try {
      var statements = constructor.getBody().getStatements();
      int start = 0;
      boolean delegated = false;
      if (!statements.isEmpty()
          && statements.get(0) instanceof CtInvocation<?> invocation
          && invocation.getExecutable().isConstructor()) {
        var target = ExternalSpecifications.resolve(invocation.getExecutable());
        delegated =
            target instanceof CtConstructor<?> other
                && other.getDeclaringType().equals(constructor.getDeclaringType());
        state = statement(invocation, state, scope).state();
        start = 1;
      }
      if (!delegated) state = initializers(constructor.getDeclaringType(), state, scope);
      var flow = new Flow(state, Exit.NORMAL);
      for (int i = start; i < statements.size() && flow.exit() == Exit.NORMAL; i++)
        flow = statement(statements.get(i), flow.state(), scope);
      var after = flow.state();
      // Normal early returns are constructor exits too; keep their partially initialized heap.
      for (var returned : constructor.getBody().getElements(new TypeFilter<>(CtReturn.class)))
        if (returned.getParent(CtExecutable.class) == constructor)
          after = facts(returned).map(LatteFacts::after).map(after::join).orElse(after);
      return after;
    } finally {
      activeConstructors.remove(constructor);
    }
  }

  private Evaluation construct(
      CtExecutable<?> declaration,
      Reference receiver,
      List<CtExpression<?>> nodes,
      List<Reference> values,
      FlowState before,
      CtElement call) {
    var state = arguments(nodes, values, declaration, before, call);
    if (!(declaration instanceof CtConstructor<?> constructor)
        || declaration.getBody() == null
        || ExternalSpecifications.isSpecification(declaration)) {
      var roots = values.stream().reduce(receiver, Reference::join);
      return new Evaluation(state.havocHeap(roots, Annotations.key(call)), receiver);
    }
    var bindings = state.variables();
    var context = new EvaluationContext(allocation("call:", call));
    evaluations.push(context);
    try {
      state = state.bind("this", receiver);
      for (int i = 0; i < Math.min(values.size(), declaration.getParameters().size()); i++) {
        var parameter = declaration.getParameters().get(i);
        var argument = values.get(i);
        if (Annotations.permission(parameter) == Permission.FREE
            && before.transferablePermission(argument) == Permission.FREE) {
          var moved = state.moveFrom(before, argument, context.site() + ":argument:" + i);
          trackMovedObservers(moved);
          state = moved.state();
          argument = moved.value();
          context.transfers().putAll(moved.origins());
          moved
              .origins()
              .forEach(
                  (owned, original) -> {
                    if (closures.containsKey(original)) closures.put(owned, closures.get(original));
                  });
        }
        state = state.bind(Annotations.key(parameter), argument);
      }
      state =
          constructorBody(
              constructor, state, new Scope(declaration, receiver.objects().iterator().next()));
      var restored = new HashMap<>(bindings);
      var shadowed = new HashSet<String>();
      shadowed.add("this");
      declaration.getParameters().forEach(p -> shadowed.add(Annotations.key(p)));
      // Repeated allocations may rename caller aliases to their older-instance summaries.
      for (String key : bindings.keySet())
        if (!shadowed.contains(key))
          restored.put(key, state.variables().getOrDefault(key, bindings.get(key)));
      return new Evaluation(state.withVariables(restored), receiver);
    } finally {
      evaluations.pop();
      if (evaluations.isEmpty()) new ConstructionFacts(context.transfers()).attach(call);
      else {
        evaluations.peek().transfers().putAll(context.transfers());
        evaluations.peek().mutations().addAll(context.mutations());
      }
    }
  }

  private Flow statement(CtStatement statement, FlowState before, Scope scope) {
    if (statement == null) return new Flow(before, Exit.NORMAL);
    Flow result;
    if (statement instanceof CtBlock<?> block) {
      result = new Flow(before, Exit.NORMAL);
      for (var child : block.getStatements()) {
        if (result.exit() != Exit.NORMAL) break;
        result = statement(child, result.state(), scope);
      }
    } else if (statement instanceof CtLocalVariable<?> local) {
      var evaluated = expression(local.getDefaultExpression(), before, scope);
      var value =
          local.getDefaultExpression() == null && !scalar(local.getType())
              ? Reference.object("uninitialized:" + Annotations.key(local))
              : evaluated.value();
      var after = evaluated.state();
      if (local.getDefaultExpression() == null) after = after.allocate(value, Permission.BOTTOM);
      if (Annotations.has(local, "Shared")) after = escape(value, after, local);
      after = after.bind(Annotations.key(local), value);
      remember(local, new LatteFacts(before, after, value));
      return new Flow(after, Exit.NORMAL);
    } else if (statement instanceof CtIf conditional) {
      var guard = expression(conditional.getCondition(), before, scope);
      var yes =
          statement(
              conditional.getThenStatement(),
              refineNull(conditional.getCondition(), guard.state(), true),
              scope);
      var no =
          statement(
              conditional.getElseStatement(),
              refineNull(conditional.getCondition(), guard.state(), false),
              scope);
      result = merge(yes, no);
    } else if (statement instanceof CtLoop loop) {
      result = loop(loop, before, scope);
    } else if (statement instanceof CtReturn<?> returned) {
      var evaluated = expression(returned.getReturnedExpression(), before, scope);
      if (activeClosures.isEmpty()) defer(evaluated.value(), evaluated.state(), false);
      if (activeClosures.isEmpty() && hasBorrowedCaptures(evaluated.value(), evaluated.state()))
        error("A closure capturing a borrowed reference cannot leave its scope", returned);
      var after =
          transfer(
              evaluated.value(),
              Annotations.permission(scope.executable()),
              Annotations.permission(scope.executable()) == Permission.FREE
                  ? releaseUnobservable(
                      List.of(evaluated.value()),
                      evaluated.value(),
                      evaluated.state(),
                      returned,
                      scope)
                  : evaluated.state(),
              returned);
      result = new Flow(after, Exit.RETURN);
    } else if (statement instanceof CtYieldStatement yielded) {
      var evaluated = expression(yielded.getExpression(), before, scope);
      result = new Flow(evaluated.state(), Exit.RETURN);
    } else if (statement instanceof CtThrow thrown) {
      var evaluated = expression(thrown.getThrownExpression(), before, scope);
      result = new Flow(escape(evaluated.value(), evaluated.state(), thrown), Exit.THROW);
    } else if (statement instanceof CtBreak) {
      result = new Flow(before, Exit.BREAK);
    } else if (statement instanceof CtContinue) {
      result = new Flow(before, Exit.CONTINUE);
    } else if (statement instanceof CtTry tried) {
      var resourceState = before;
      if (tried instanceof CtTryWithResource resources)
        for (var resource : resources.getResources())
          if (resource instanceof CtStatement s)
            resourceState = statement(s, resourceState, scope).state();
      var body = statement(tried.getBody(), resourceState, scope);
      var exceptional = exceptionalStates(tried.getBody(), before.join(body.state()));
      result = body;
      for (var catcher : tried.getCatchers()) {
        var caught = Reference.object(Annotations.key(catcher.getParameter()));
        var catchState =
            exceptional
                .allocate(caught, Permission.SHARED)
                .bind(Annotations.key(catcher.getParameter()), caught);
        result = merge(result, statement(catcher.getBody(), catchState, scope));
      }
      if (tried.getFinalizer() != null) {
        var finalized = statement(tried.getFinalizer(), result.state().join(exceptional), scope);
        result =
            new Flow(
                finalized.state(),
                finalized.exit() == Exit.NORMAL ? result.exit() : finalized.exit());
      }
    } else if (statement instanceof CtSynchronized synchronizedBlock) {
      var lock = expression(synchronizedBlock.getExpression(), before, scope);
      result = statement(synchronizedBlock.getBlock(), lock.state(), scope);
    } else if (statement instanceof CtSwitch<?> switched) {
      var evaluated = expression(switched.getSelector(), before, scope);
      var merged = evaluated.state();
      // Include every prefix and the no-case path, conservatively accounting for fall-through.
      for (var branch : switched.getCases()) {
        var branchState = merged;
        for (var child : branch.getStatements())
          branchState = statement(child, branchState, scope).state();
        merged = merged.join(branchState);
      }
      result = new Flow(merged, Exit.NORMAL);
    } else if (statement instanceof CtExpression<?> expression) {
      var evaluated = expression(expression, before, scope);
      return new Flow(evaluated.state(), Exit.NORMAL);
    } else if (statement instanceof CtAssert<?> assertion) {
      var condition = expression(assertion.getAssertExpression(), before, scope);
      result = new Flow(condition.state(), Exit.NORMAL);
    } else if (statement instanceof CtType<?> || statement instanceof CtComment) {
      result = new Flow(before, Exit.NORMAL);
    } else {
      problem(
          "Unsupported ownership statement: " + statement.getClass().getSimpleName(), statement);
      result = new Flow(before, Exit.NORMAL);
    }
    if (statement.getLabel() != null && result.exit() == Exit.BREAK)
      result = new Flow(result.state(), Exit.NORMAL);
    remember(statement, new LatteFacts(before, result.state(), Reference.scalarValue()));
    return result;
  }

  private FlowState exceptionalStates(CtBlock<?> body, FlowState state) {
    for (var element : body.getElements(new TypeFilter<>(CtElement.class))) {
      if (element instanceof CtThrow
          || element instanceof CtInvocation<?>
          || element instanceof CtConstructorCall<?>
          || element instanceof CtArrayAccess<?, ?>
          || element instanceof CtReturn<?>)
        state = facts(element).map(LatteFacts::after).map(state::join).orElse(state);
    }
    return state;
  }

  private FlowState refineNull(CtExpression<?> condition, FlowState state, boolean truth) {
    if (!(condition instanceof CtBinaryOperator<?> comparison)) return state;
    boolean equality = comparison.getKind() == BinaryOperatorKind.EQ;
    if (!equality && comparison.getKind() != BinaryOperatorKind.NE) return state;
    if (equality != truth) return state;
    var left = comparison.getLeftHandOperand();
    var right = comparison.getRightHandOperand();
    CtExpression<?> target =
        right instanceof CtLiteral<?> literal && literal.getValue() == null
            ? left
            : left instanceof CtLiteral<?> literal && literal.getValue() == null ? right : null;
    if (target instanceof CtFieldRead<?> field) {
      var receiver = facts(field.getTarget()).map(LatteFacts::value).orElse(Reference.nullValue());
      return state.write(receiver, field.getVariable().getQualifiedName(), Reference.nullValue());
    }
    if (target instanceof CtVariableRead<?> read && read.getVariable().getDeclaration() != null)
      return state.bind(
          Annotations.key(read.getVariable().getDeclaration()), Reference.nullValue());
    return state;
  }

  private Flow merge(Flow left, Flow right) {
    if (left.exit() == Exit.NORMAL && right.exit() != Exit.NORMAL) return left;
    if (right.exit() == Exit.NORMAL && left.exit() != Exit.NORMAL) return right;
    return new Flow(
        left.state().join(right.state()), left.exit() == right.exit() ? left.exit() : Exit.NORMAL);
  }

  private Flow loop(CtLoop loop, FlowState before, Scope scope) {
    var entry = before;
    Reference iteratedArray = null;
    if (loop instanceof CtFor forLoop)
      for (var init : forLoop.getForInit()) entry = statement(init, entry, scope).state();
    if (loop instanceof CtForEach each) {
      var collection = expression(each.getExpression(), entry, scope);
      if (each.getExpression().getType() instanceof CtArrayTypeReference<?>) {
        iteratedArray = collection.value();
        entry = collection.state().ensureArray(iteratedArray);
      } else {
        var value =
            scalar(each.getVariable().getType())
                ? Reference.scalarValue()
                : Reference.object("element:" + Annotations.key(each));
        entry =
            collection
                .state()
                .allocate(value, Permission.SHARED)
                .bind(Annotations.key(each.getVariable()), value);
        remember(each.getVariable(), new LatteFacts(collection.state(), entry, value));
      }
    }
    var head = entry;
    for (int iteration = 0; iteration < 128; iteration++) {
      var iterationState = head;
      if (iteratedArray != null && loop instanceof CtForEach each) {
        var value =
            scalar(each.getVariable().getType())
                ? Reference.scalarValue()
                : head.read(iteratedArray, ArraySlots.ANY).orElse(Reference.nullValue());
        iterationState = head.bind(Annotations.key(each.getVariable()), value);
        remember(each.getVariable(), new LatteFacts(head, iterationState, value));
      }
      var condition =
          loop instanceof CtWhile w
              ? w.getLoopingExpression()
              : loop instanceof CtDo d
                  ? d.getLoopingExpression()
                  : loop instanceof CtFor f ? f.getExpression() : null;
      var evaluated =
          loop instanceof CtDo
              ? new Evaluation(iterationState, Reference.scalarValue())
              : expression(condition, iterationState, scope);
      var body = statement(loop.getBody(), evaluated.state(), scope);
      var tail = body.state();
      for (var abrupt :
          loop.getBody() == null
              ? List.<CtStatement>of()
              : loop.getBody().getElements(new TypeFilter<>(CtStatement.class)))
        if (abrupt instanceof CtBreak || abrupt instanceof CtContinue)
          tail = facts(abrupt).map(LatteFacts::after).map(tail::join).orElse(tail);
      if (loop instanceof CtFor forLoop)
        for (var update : forLoop.getForUpdate()) tail = statement(update, tail, scope).state();
      if (loop instanceof CtDo) tail = expression(condition, tail, scope).state();
      var joined = head.join(tail).join(entry);
      if (joined.equals(head)) return new Flow(joined, Exit.NORMAL);
      head = joined;
    }
    problem("Ownership loop analysis did not converge", loop);
    return new Flow(head, Exit.NORMAL);
  }

  private Evaluation expression(CtExpression<?> expression, FlowState before, Scope scope) {
    if (expression == null) return new Evaluation(before, Reference.scalarValue());
    Evaluation result;
    if (expression instanceof CtLiteral<?> literal) {
      result =
          new Evaluation(
              before, literal.getValue() == null ? Reference.nullValue() : Reference.scalarValue());
    } else if (expression instanceof CtThisAccess<?> || expression instanceof CtSuperAccess<?>) {
      result = new Evaluation(before, Reference.object(scope.receiver()));
    } else if (expression instanceof CtFieldAccess<?> field) {
      result = field(field, before, scope);
    } else if (expression instanceof CtVariableAccess<?> variable) {
      var declaration = variable.getVariable().getDeclaration();
      String key =
          declaration == null
              ? variable.getVariable().getSimpleName()
              : Annotations.key(declaration);
      var value = before.variables().get(key);
      if (value == null) {
        value =
            scalar(variable.getType())
                ? Reference.scalarValue()
                : Reference.object("unknown:" + key);
        result = new Evaluation(before.allocate(value, Permission.SHARED), value);
      } else {
        if (before.permission(value) == Permission.BOTTOM)
          error("Use of a consumed or uninitialized reference", expression);
        result = new Evaluation(before, value);
      }
    } else if (expression instanceof CtAssignment<?, ?> assignment) {
      var target = assignment.getAssigned();
      // Java evaluates the target (including array index) before the RHS.
      var targetEvaluation =
          target instanceof CtVariableWrite<?> && !(target instanceof CtFieldWrite<?>)
              ? new Evaluation(before, Reference.scalarValue())
              : expression(target, before, scope);
      var evaluated = expression(assignment.getAssignment(), targetEvaluation.state(), scope);
      var after = evaluated.state();
      if (target instanceof CtFieldWrite<?> field) {
        var receiver =
            facts(field.getTarget())
                .map(LatteFacts::value)
                .orElse(Reference.object(scope.receiver()));
        var declaration = ExternalSpecifications.resolve(field.getVariable());
        var expected = Annotations.permission(declaration);
        if (!receiver.scalar() && after.permission(receiver) == Permission.SHARED)
          expected = Permission.SHARED;
        mutation(receiver, after);
        after =
            transfer(evaluated.value(), expected, after, assignment)
                .write(receiver, field.getVariable().getQualifiedName(), evaluated.value());
      } else if (target instanceof CtVariableWrite<?> variable) {
        var declaration = variable.getVariable().getDeclaration();
        after =
            after.bind(
                declaration == null
                    ? variable.getVariable().getSimpleName()
                    : Annotations.key(declaration),
                evaluated.value());
      } else if (target instanceof CtArrayWrite<?> array) {
        var receiver =
            facts(array.getTarget()).map(LatteFacts::value).orElse(Reference.nullValue());
        if (after.permission(receiver) == Permission.SHARED)
          after = escape(evaluated.value(), after, assignment);
        mutation(receiver, after);
        after = after.write(receiver, ArraySlots.at(array.getIndexExpression()), evaluated.value());
      }
      result = new Evaluation(after, evaluated.value());
    } else if (expression instanceof CtConstructorCall<?> call) {
      var state = before;
      var arguments = new ArrayList<Reference>();
      for (var argument : call.getArguments()) {
        var evaluated = expression(argument, state, scope);
        state = evaluated.state();
        arguments.add(evaluated.value());
      }
      var declaration = ExternalSpecifications.resolve(call.getExecutable());
      var value = Reference.object(allocation("new:", call));
      state = state.fresh(value, Permission.FREE);
      var allocatedType =
          call instanceof CtNewClass<?> anonymous
              ? anonymous.getAnonymousClass().getReference()
              : call.getType();
      if (mayFinalize(allocatedType, true)) finalizableObjects.addAll(value.objects());
      var constructor = declaration instanceof CtConstructor<?> c ? c : null;
      if (constructor != null) state = defaults(constructor.getDeclaringType(), value, state);
      state = releaseArguments(arguments, value, declaration, state, call, scope);
      result = construct(declaration, value, call.getArguments(), arguments, state, call);
    } else if (expression instanceof CtInvocation<?> call) {
      var receiver =
          call.getExecutable().isConstructor()
              ? new Evaluation(before, Reference.object(scope.receiver()))
              : expression(call.getTarget(), before, scope);
      var state = receiver.state();
      var arguments = new ArrayList<Reference>();
      for (var argument : call.getArguments()) {
        var evaluated = expression(argument, state, scope);
        state = evaluated.state();
        arguments.add(evaluated.value());
      }
      var declaration = ExternalSpecifications.resolve(call.getExecutable());
      state = releaseArguments(arguments, receiver.value(), declaration, state, call, scope);
      var localClosure =
          call.getExecutable().isConstructor()
              ? null
              : invokeClosure(call, receiver.value(), arguments, state);
      if (localClosure != null) {
        remember(expression, new LatteFacts(before, localClosure.state(), localClosure.value()));
        return localClosure;
      }
      if (call.getExecutable().isConstructor()) {
        result =
            call.getExecutable().getDeclaringType() != null
                    && call.getExecutable()
                        .getDeclaringType()
                        .getQualifiedName()
                        .equals("java.lang.Object")
                ? new Evaluation(state, receiver.value())
                : construct(
                    declaration, receiver.value(), call.getArguments(), arguments, state, call);
        remember(expression, new LatteFacts(before, result.state(), result.value()));
        return result;
      } else if (declaration == null) {
        state = escape(receiver.value(), state, call);
        for (var argument : arguments) state = escape(argument, state, call);
      } else {
        var distinct = new ArrayList<Reference>();
        if (!call.getExecutable().isStatic()) distinct.add(receiver.value());
        for (int i = 0; i < Math.min(arguments.size(), declaration.getParameters().size()); i++)
          if (Annotations.permission(declaration.getParameters().get(i)).refinable())
            distinct.add(arguments.get(i));
        distinct(distinct, state, call);
        state = arguments(call.getArguments(), arguments, declaration, state, call);
      }
      if (declaration != null && MutationEffects.on(declaration).mutates()) {
        var effects = MutationEffects.on(declaration);
        var roots =
            effects.receiver() || effects.globals() ? receiver.value() : Reference.nullValue();
        for (int i = 0; i < arguments.size(); i++)
          if (effects.parameter(i) || effects.globals()) roots = roots.join(arguments.get(i));
        mutation(roots, state);
        state = state.havocHeap(roots, Annotations.key(call));
        defer(roots, state, false);
      }
      result = returned(call, declaration, receiver.value(), arguments, state);
    } else if (expression instanceof CtNewArray<?> array) {
      var state = before;
      var elements = new ArrayList<Reference>();
      for (var dimension : array.getDimensionExpressions())
        state = expression(dimension, state, scope).state();
      for (var child : array.getElements()) {
        var evaluated = expression(child, state, scope);
        state = evaluated.state();
        elements.add(evaluated.value());
      }
      var value = Reference.object(allocation("array:", array));
      state = state.fresh(value, Permission.FREE);
      var component = ((CtArrayTypeReference<?>) array.getType()).getComponentType();
      if (array.getDimensionExpressions().size() > 1) {
        // Implicit inner arrays are summarized; explicit nested initializers retain their
        // identities.
        state = state.ensureArray(value);
      } else {
        state =
            state.initializeArray(
                value, scalar(component) ? Reference.scalarValue() : Reference.nullValue());
      }
      for (int i = 0; i < elements.size(); i++)
        state = state.write(value, ArraySlots.at(i), elements.get(i));
      result = new Evaluation(state, value);
    } else if (expression instanceof CtArrayAccess<?, ?> array) {
      var receiver = expression(array.getTarget(), before, scope);
      var index = expression(array.getIndexExpression(), receiver.state(), scope);
      var state = index.state().ensureArray(receiver.value());
      var value =
          scalar(array.getType())
              ? Reference.scalarValue()
              : state
                  .read(receiver.value(), ArraySlots.at(array.getIndexExpression()))
                  .orElse(Reference.nullValue());
      if (array instanceof CtArrayRead<?> && state.permission(value) == Permission.BOTTOM)
        error("Use of a consumed or uninitialized reference", array);
      result = new Evaluation(state, value);
    } else if (expression instanceof CtSwitchExpression<?, ?> switched) {
      var selector = expression(switched.getSelector(), before, scope);
      var after = selector.state();
      var value = Reference.none();
      for (var branch : switched.getCases()) {
        var flow = new Flow(selector.state(), Exit.NORMAL);
        for (var child : branch.getStatements()) {
          if (flow.exit() != Exit.NORMAL) break;
          flow = statement(child, flow.state(), scope);
        }
        after = after.join(flow.state());
        for (var yielded : branch.getElements(new TypeFilter<>(CtYieldStatement.class)))
          value =
              value.join(
                  facts(yielded.getExpression())
                      .map(LatteFacts::value)
                      .orElse(Reference.nullValue()));
      }
      result = new Evaluation(after, scalar(switched.getType()) ? Reference.scalarValue() : value);
    } else if (expression instanceof CtConditional<?> conditional) {
      var guard = expression(conditional.getCondition(), before, scope);
      var yes = expression(conditional.getThenExpression(), guard.state(), scope);
      var no = expression(conditional.getElseExpression(), guard.state(), scope);
      result = new Evaluation(yes.state().join(no.state()), yes.value().join(no.value()));
    } else if (expression instanceof CtBinaryOperator<?> binary) {
      var left = expression(binary.getLeftHandOperand(), before, scope);
      var right = expression(binary.getRightHandOperand(), left.state(), scope);
      var state =
          binary.getKind() == BinaryOperatorKind.AND || binary.getKind() == BinaryOperatorKind.OR
              ? left.state().join(right.state())
              : right.state();
      result = new Evaluation(state, Reference.scalarValue());
    } else if (expression instanceof CtUnaryOperator<?> unary) {
      result =
          new Evaluation(
              expression(unary.getOperand(), before, scope).state(), Reference.scalarValue());
    } else if (expression instanceof CtLambda<?>
        || expression instanceof CtExecutableReferenceExpression<?, ?>) {
      var state = before;
      var captured = new HashMap<String, Reference>();
      if (expression instanceof CtExecutableReferenceExpression<?, ?> reference
          && !(reference.getTarget() instanceof CtTypeAccess<?>)) {
        var evaluated = expression(reference.getTarget(), state, scope);
        state = evaluated.state();
        captured.put(ClosureFacts.RECEIVER, evaluated.value());
      } else if (expression instanceof CtLambda<?>) {
        for (var read : expression.getElements(new TypeFilter<>(CtVariableRead.class))) {
          var declaration = read.getVariable().getDeclaration();
          if (declaration != null && state.variables().containsKey(Annotations.key(declaration)))
            captured.put(
                Annotations.key(declaration), state.variables().get(Annotations.key(declaration)));
        }
        if (!expression.getElements(new TypeFilter<>(CtThisAccess.class)).isEmpty())
          captured.put("this", Reference.object(scope.receiver()));
      }
      var value = Reference.object(allocation("closure:", expression));
      state = state.fresh(value, Permission.FREE);
      for (var capture : captured.entrySet())
        state = state.write(value, ClosureFacts.CAPTURE + capture.getKey(), capture.getValue());
      closures.put(value.objects().iterator().next(), new Closure(expression, scope));
      var previous = ClosureFacts.on(expression).orElse(new ClosureFacts(Set.of(), false, false));
      new ClosureFacts(captured.keySet(), previous.deferred(), previous.escaped())
          .attach(expression);
      // Check the body in isolation. Creating a closure never executes it or transfers captures.
      if (expression instanceof CtLambda<?> lambda) {
        var mutations =
            evaluations.isEmpty() ? Set.<String>of() : Set.copyOf(evaluations.peek().mutations());
        var deferred = state;
        var contract = lambda.getOverriddenMethod();
        for (int i = 0; i < lambda.getParameters().size(); i++) {
          var parameter = lambda.getParameters().get(i);
          var specified =
              contract != null && i < contract.getParameters().size()
                  ? contract.getParameters().get(i)
                  : parameter;
          var parameterValue =
              scalar(parameter.getType())
                  ? Reference.scalarValue()
                  : Reference.object(Annotations.key(parameter));
          var next =
              deferred
                  .allocate(parameterValue, Annotations.permission(specified))
                  .bind(Annotations.key(parameter), parameterValue);
          remember(parameter, new LatteFacts(deferred, next, parameterValue));
          deferred = next;
        }
        var nested = new Scope(contract == null ? lambda : contract, scope.receiver());
        if (lambda.getBody() != null) statement(lambda.getBody(), deferred, nested);
        else {
          var evaluated = expression(lambda.getExpression(), deferred, nested);
          transfer(evaluated.value(), Annotations.permission(contract), evaluated.state(), lambda);
        }
        if (!evaluations.isEmpty()) {
          evaluations.peek().mutations().clear();
          evaluations.peek().mutations().addAll(mutations);
        }
      }
      result = new Evaluation(state, value);
    } else if (expression instanceof CtTypeAccess<?>) {
      result = new Evaluation(before, Reference.scalarValue());
    } else {
      problem(
          "Unsupported ownership expression: " + expression.getClass().getSimpleName(), expression);
      var value = Reference.object("unsupported:" + Annotations.key(expression));
      result = new Evaluation(before.allocate(value, Permission.SHARED), value);
    }
    remember(expression, new LatteFacts(before, result.state(), result.value()));
    return result;
  }

  private Evaluation field(CtFieldAccess<?> field, FlowState before, Scope scope) {
    var receiver = expression(field.getTarget(), before, scope);
    var reference = field.getVariable();
    var owner =
        reference.isStatic()
            ? Reference.object("static:" + reference.getDeclaringType())
            : receiver.value();
    if (scalar(field.getType())) return new Evaluation(receiver.state(), Reference.scalarValue());
    var state = receiver.state();
    var value = Reference.none();
    for (String identity : owner.objects()) {
      var singleton = Reference.object(identity);
      var existing = state.read(singleton, reference.getQualifiedName());
      if (existing.isPresent()) {
        value = value.join(existing.get());
        continue;
      }
      var child = Reference.object(identity + "." + reference.getQualifiedName());
      if (mayFinalize(field.getType())) finalizableObjects.addAll(child.objects());
      var permission = Annotations.permission(ExternalSpecifications.resolve(reference));
      if (reference.isStatic() || state.permission(singleton) == Permission.SHARED)
        permission = Permission.SHARED;
      state =
          state.allocate(child, permission).write(singleton, reference.getQualifiedName(), child);
      value = value.join(child);
    }
    return new Evaluation(state, value);
  }

  private Evaluation returned(
      CtInvocation<?> call,
      CtExecutable<?> declaration,
      Reference receiver,
      List<Reference> arguments,
      FlowState state) {
    if (scalar(JavaTypes.result(call))) return new Evaluation(state, Reference.scalarValue());
    var permission = Annotations.permission(declaration);
    var fresh = Reference.object(allocation("return:", call));
    if (mayFinalize(JavaTypes.result(call))) finalizableObjects.addAll(fresh.objects());
    if (permission == Permission.FREE) return new Evaluation(state.fresh(fresh, permission), fresh);
    var paths = returnAliases.paths(declaration);
    Reference value = Reference.none();
    for (var path : paths) {
      Reference current;
      if (path.root() == ReturnAliases.NULL) {
        current = Reference.nullValue();
      } else if (path.root() == ReturnAliases.FRESH) {
        current = fresh;
        state = state.allocate(fresh, permission);
      } else if (path.root() == ReturnAliases.EXTERNAL) {
        current = fresh;
        state = state.allocate(fresh, Permission.SHARED);
      } else
        current =
            path.root() == ReturnAliases.RECEIVER
                ? receiver
                : path.root() < arguments.size()
                    ? arguments.get(path.root())
                    : Reference.nullValue();
      for (String field : path.fields()) {
        if (field.equals("*")) {
          var unknown = Reference.object("reachable:" + Annotations.key(call) + ":" + path.root());
          var descendants = new HashSet<>(state.reachable(current));
          var candidateState = state;
          if (permission == Permission.UNIQUE)
            descendants.removeIf(
                id ->
                    candidateState.permissions().get(id) != Permission.UNIQUE
                        && candidateState.permissions().get(id) != Permission.FREE);
          state =
              state.allocate(
                  unknown,
                  state.permission(current) == Permission.SHARED ? Permission.SHARED : permission);
          current = new Reference(descendants, false).join(unknown);
        } else {
          if (ArraySlots.isSlot(field)) state = state.ensureArray(current);
          var existing = state.read(current, field);
          if (existing.isPresent()) current = existing.get();
          else {
            var child =
                new Reference(
                    current.objects().stream()
                        .map(id -> id + "." + field)
                        .collect(java.util.stream.Collectors.toSet()),
                    false);
            var childPermission =
                state.permission(current) == Permission.SHARED
                    ? Permission.SHARED
                    : fieldPermissions.getOrDefault(field, Permission.SHARED);
            state = state.allocate(child, childPermission).write(current, field, child);
            current = child;
          }
        }
      }
      value = value.join(current);
    }
    if (paths.isEmpty()
        && (declaration == null
            || declaration.getBody() == null
            || ExternalSpecifications.isSpecification(declaration))) {
      value = fresh.join(receiver);
      for (var argument : arguments) value = value.join(argument);
      state = state.allocate(fresh, permission);
    }
    if (permission == Permission.SHARED) state = state.changeReachable(value, Permission.SHARED);
    return new Evaluation(state, value);
  }

  private FlowState arguments(
      List<CtExpression<?>> nodes,
      List<Reference> values,
      CtExecutable<?> declaration,
      FlowState state,
      CtElement call) {
    if (declaration == null) {
      for (var value : values) state = escape(value, state, call);
      return state;
    }
    var exclusive = new ArrayList<Reference>();
    for (int i = 0; i < Math.min(values.size(), declaration.getParameters().size()); i++)
      if (Annotations.permission(declaration.getParameters().get(i)).refinable())
        exclusive.add(values.get(i));
    distinct(exclusive, state, call);
    for (int i = 0; i < values.size(); i++) {
      var expected =
          i < declaration.getParameters().size()
              ? Annotations.permission(declaration.getParameters().get(i))
              : Permission.SHARED;
      if (expected != Permission.BORROWED && hasBorrowedCaptures(values.get(i), state))
        error(
            "A closure capturing a borrowed reference requires a borrowed parameter", nodes.get(i));
      state = transfer(values.get(i), expected, state, nodes.get(i));
    }
    return state;
  }

  private void distinct(List<Reference> values, FlowState state, CtElement call) {
    for (int i = 0; i < values.size(); i++)
      for (int j = i + 1; j < values.size(); j++)
        if (state.overlaps(values.get(i), values.get(j)))
          error("Non-distinct parameters or receiver in call", call);
  }

  private FlowState transfer(
      Reference value, Permission expected, FlowState state, CtElement site) {
    if (value.scalar() || value.objects().isEmpty()) return state;
    var actual = state.transferablePermission(value);
    if (expected == Permission.SHARED) return escape(value, state, site);
    boolean accepted =
        switch (expected) {
          case FREE -> actual == Permission.FREE;
          case UNIQUE -> actual == Permission.FREE || actual == Permission.UNIQUE;
          case BORROWED -> actual.refinable();
          case IMMUTABLE -> actual == Permission.IMMUTABLE;
          case BOTTOM -> false;
          case SHARED -> true;
        };
    if (!accepted) {
      error("Expected " + expected + " but got " + actual, site);
      return state;
    }
    return expected == Permission.FREE
        ? state.changeReachable(value, Permission.BOTTOM)
        : expected == Permission.UNIQUE ? state.allocate(value, Permission.UNIQUE) : state;
  }

  private FlowState releaseArguments(
      List<Reference> values,
      Reference receiver,
      CtExecutable<?> declaration,
      FlowState state,
      CtElement call,
      Scope scope) {
    if (declaration == null) return state;
    var moved = new ArrayList<Reference>();
    for (int i = 0; i < Math.min(values.size(), declaration.getParameters().size()); i++)
      if (Annotations.permission(declaration.getParameters().get(i)) == Permission.FREE)
        moved.add(values.get(i));
    // Arguments and the receiver are already evaluated, but remain observable during the call.
    var retained = values.stream().reduce(receiver, Reference::join);
    return releaseUnobservable(moved, retained, state, call, scope);
  }

  private FlowState releaseUnobservable(
      List<Reference> values, Reference retained, FlowState state, CtElement site, Scope scope) {
    if (!evaluations.isEmpty() || site.getParent(CtExecutable.class) != scope.executable())
      return state;
    var boundary = LocalObservability.boundary(site);
    if (boundary.isEmpty()) return state;
    var candidates =
        values.stream().filter(value -> state.permission(value) == Permission.UNIQUE).toList();
    if (candidates.isEmpty()) return state;
    var live =
        futureUses.computeIfAbsent(
            boundary.get(),
            statement -> LocalObservability.after(statement, scope.executable(), annotationNames));
    var roots = retained.join(Reference.object(scope.receiver()));
    for (var parameter : scope.executable().getParameters())
      if (!scalar(parameter.getType()) && Annotations.permission(parameter) != Permission.FREE)
        roots = roots.join(Reference.object(Annotations.key(parameter)));
    for (var binding : state.variables().entrySet())
      if (live.contains(binding.getKey())) roots = roots.join(binding.getValue());
    for (var entry : state.permissions().entrySet())
      if (entry.getValue() == Permission.SHARED
          || entry.getValue() == Permission.BORROWED
          || finalizableObjects.contains(entry.getKey()))
        roots = roots.join(Reference.object(entry.getKey()));
    var observable = state.reachable(roots);
    var released = new HashSet<FlowState.Field>();
    for (var value : candidates) {
      var region = state.reachable(value);
      // A live outside owner of any descendant also prevents moving the whole region.
      boolean outsideOwner =
          state.heap().entrySet().stream()
              .anyMatch(
                  edge ->
                      !region.contains(edge.getKey().owner())
                          && observable.contains(edge.getKey().owner())
                          && !Collections.disjoint(edge.getValue().objects(), region));
      if (outsideOwner) continue;
      state
          .heap()
          .forEach(
              (field, contents) -> {
                var permission = fieldPermissions.get(field.name());
                if (!observable.contains(field.owner())
                    && (permission == Permission.UNIQUE
                        || permission == Permission.FREE
                        || field.name().startsWith(ClosureFacts.CAPTURE))
                    && !Collections.disjoint(contents.objects(), region)) released.add(field);
              });
    }
    if (released.isEmpty()) return state;
    var accumulated = new HashSet<>(released);
    OwnershipRelease.on(site).ifPresent(facts -> accumulated.addAll(facts.fields()));
    new OwnershipRelease(accumulated).attach(site);
    return state.withoutFields(released);
  }

  private boolean mayFinalize(CtTypeReference<?> type) {
    return mayFinalize(type, false);
  }

  private void trackMovedObservers(FlowState.Move moved) {
    moved
        .origins()
        .forEach(
            (owned, original) -> {
              if (finalizableObjects.contains(original)) finalizableObjects.add(owned);
            });
  }

  private boolean mayFinalize(CtTypeReference<?> type, boolean exactType) {
    if (type == null || scalar(type) || type instanceof CtArrayTypeReference<?>) return false;
    var declaration = type.getTypeDeclaration();
    if (declaration == null) return true;
    // A value obtained from a parameter, field or opaque result may be a subtype with a finalizer.
    if (!exactType && !declaration.hasModifier(ModifierKind.FINAL)) return true;
    return declaration.getAllMethods().stream()
        .anyMatch(
            method ->
                method.getSimpleName().equals("finalize")
                    && method.getParameters().isEmpty()
                    && !method.getDeclaringType().getQualifiedName().equals("java.lang.Object"));
  }

  private FlowState escape(Reference value, FlowState state, CtElement site) {
    if (value.scalar()) return state;
    defer(value, state, true);
    if (state.reachable(value).stream()
        .anyMatch(id -> state.permissions().get(id) == Permission.BORROWED))
      error("Borrowed reference may escape its scope", site);
    return state.changeReachable(value, Permission.SHARED);
  }

  private Closure closure(String identity) {
    return closures.get(identity.startsWith("summary:") ? identity.substring(8) : identity);
  }

  private void mutation(Reference value, FlowState state) {
    if (!evaluations.isEmpty()) evaluations.peek().mutations().addAll(state.reachable(value));
  }

  private void defer(Reference value, FlowState state, boolean escaped) {
    for (String identity : state.reachable(value)) {
      var closure = closure(identity);
      if (closure == null) continue;
      var facts = ClosureFacts.on(closure.expression()).orElseThrow();
      new ClosureFacts(facts.bindings(), true, escaped || facts.escaped())
          .attach(closure.expression());
    }
  }

  private boolean hasBorrowedCaptures(Reference value, FlowState state) {
    return state.reachable(value).stream()
        .filter(id -> closure(id) != null)
        .anyMatch(
            id ->
                state.reachable(Reference.object(id)).stream()
                    .anyMatch(
                        capture ->
                            !capture.equals(id)
                                && state.permissions().get(capture) == Permission.BORROWED));
  }

  private Evaluation invokeClosure(
      CtInvocation<?> call, Reference receiver, List<Reference> arguments, FlowState before) {
    if (receiver.objects().isEmpty()
        || receiver.objects().stream().anyMatch(id -> closure(id) == null)) return null;
    if (receiver.objects().stream()
        .anyMatch(
            id -> {
              var functional = ClosureFacts.functional(closure(id).expression());
              return functional == null
                  || !functional.getSignature().equals(call.getExecutable().getSignature());
            })) return null;
    if (receiver.objects().stream()
        .anyMatch(
            id ->
                id.startsWith("summary:")
                    || !before.permission(Reference.object(id)).refinable())) {
      defer(receiver, before, false);
      return null;
    }
    var outcomes = new ArrayList<Evaluation>();
    var targets = new ArrayList<ClosureInvocation.Target>();
    for (String identity : receiver.objects()) {
      var closure = closure(identity);
      if (!activeClosures.add(closure.expression())) {
        problem("Recursive local callback execution requires a summary", call);
        continue;
      }
      var captures = new HashMap<String, Reference>();
      for (String key : ClosureFacts.on(closure.expression()).orElseThrow().bindings())
        captures.put(
            key,
            before
                .read(Reference.object(identity), ClosureFacts.CAPTURE + key)
                .orElse(Reference.nullValue()));
      var context = new EvaluationContext(allocation("invoke:", call));
      evaluations.push(context);
      try {
        var functional = ClosureFacts.functional(closure.expression());
        var state = arguments(call.getArguments(), arguments, functional, before, call);
        var inputTransfers = new HashMap<String, String>();
        var outputTransfers = new HashMap<String, String>();
        var effectiveArguments = new ArrayList<>(arguments);
        if (functional != null)
          for (int i = 0; i < Math.min(arguments.size(), functional.getParameters().size()); i++)
            if (Annotations.permission(functional.getParameters().get(i)) == Permission.FREE
                && before.transferablePermission(arguments.get(i)) == Permission.FREE) {
              var moved =
                  state.moveFrom(before, arguments.get(i), context.site() + ":parameter:" + i);
              trackMovedObservers(moved);
              state = moved.state();
              effectiveArguments.set(i, moved.value());
              inputTransfers.putAll(moved.origins());
              moved
                  .origins()
                  .forEach(
                      (owned, original) -> {
                        if (closures.containsKey(original))
                          closures.put(owned, closures.get(original));
                      });
            }
        for (var capture : captures.entrySet()) {
          state = state.bind(capture.getKey(), capture.getValue());
          if (state.permission(capture.getValue()) == Permission.BOTTOM)
            error("Callback captures a consumed reference", call);
        }
        var result = Reference.scalarValue();
        var returnSource = state;
        if (closure.expression() instanceof CtLambda<?> lambda) {
          for (int i = 0; i < Math.min(arguments.size(), lambda.getParameters().size()); i++) {
            var parameter = lambda.getParameters().get(i);
            state = state.bind(Annotations.key(parameter), effectiveArguments.get(i));
            remember(parameter, new LatteFacts(before, state, effectiveArguments.get(i)));
          }
          var lexical = captures.getOrDefault("this", Reference.object(closure.scope().receiver()));
          var nested =
              new Scope(
                  functional == null ? lambda : functional,
                  lexical.objects().stream().findFirst().orElse(closure.scope().receiver()));
          if (lambda.getBody() != null) {
            state = statement(lambda.getBody(), state, nested).state();
            result = Reference.nullValue();
            for (var returned : lambda.getBody().getElements(new TypeFilter<>(CtReturn.class)))
              if (returned.getParent(CtExecutable.class) == lambda) {
                var returnFacts = facts(returned);
                if (returnFacts.isPresent()) state = state.join(returnFacts.get().after());
                result =
                    result.join(
                        facts(returned.getReturnedExpression())
                            .map(LatteFacts::value)
                            .orElse(Reference.scalarValue()));
                returnSource =
                    facts(returned.getReturnedExpression())
                        .map(LatteFacts::after)
                        .map(returnSource::join)
                        .orElse(returnSource);
              }
          } else {
            var evaluated = expression(lambda.getExpression(), state, nested);
            state = evaluated.state();
            result = evaluated.value();
            returnSource = state;
            state = transfer(result, Annotations.permission(functional), state, call);
          }
          if (Annotations.permission(functional) == Permission.FREE && !result.scalar()) {
            var moved = state.moveFrom(returnSource, result, context.site() + ":result");
            trackMovedObservers(moved);
            state = moved.state();
            result = moved.value();
            outputTransfers.putAll(moved.origins());
          }
        } else if (closure.expression()
            instanceof CtExecutableReferenceExpression<?, ?> reference) {
          var declaration = ExternalSpecifications.resolve(reference.getExecutable());
          boolean unbound =
              reference.getTarget() instanceof CtTypeAccess<?>
                  && !reference.getExecutable().isStatic()
                  && !reference.getExecutable().isConstructor();
          var target =
              unbound && !effectiveArguments.isEmpty()
                  ? effectiveArguments.get(0)
                  : captures.getOrDefault(ClosureFacts.RECEIVER, Reference.nullValue());
          var values =
              unbound
                  ? effectiveArguments.subList(1, effectiveArguments.size())
                  : effectiveArguments;
          var nodes =
              unbound
                  ? call.getArguments().subList(1, call.getArguments().size())
                  : call.getArguments();
          if (reference.getExecutable().isConstructor()) {
            var allocated = Reference.object(allocation("new:", call));
            state = state.fresh(allocated, Permission.FREE);
            var constructor = declaration instanceof CtConstructor<?> c ? c : null;
            if (constructor != null)
              state = defaults(constructor.getDeclaringType(), allocated, state);
            var constructed = construct(declaration, allocated, nodes, values, state, call);
            state =
                Annotations.permission(functional) == Permission.FREE
                    ? constructed.state()
                    : transfer(
                        constructed.value(),
                        Annotations.permission(functional),
                        constructed.state(),
                        call);
            result = constructed.value();
            outputTransfers.putAll(context.transfers());
          } else {
            state = arguments(nodes, values, declaration, state, call);
            remember(reference.getTarget(), new LatteFacts(state, state, target));
            if (declaration == null) state = escape(target, state, call);
            else if (MutationEffects.on(declaration).mutates()) {
              var effect = MutationEffects.on(declaration);
              var roots = effect.receiver() || effect.globals() ? target : Reference.nullValue();
              for (int i = 0; i < values.size(); i++)
                if (effect.parameter(i) || effect.globals()) roots = roots.join(values.get(i));
              mutation(roots, state);
              state = state.havocHeap(roots, Annotations.key(call));
            }
            var returned = returned(call, declaration, target, values, state);
            state = returned.state();
            result = returned.value();
            var expected = Annotations.permission(functional);
            if (expected != Permission.FREE) state = transfer(result, expected, state, call);
            else if (Annotations.permission(declaration) != Permission.FREE && !result.scalar()) {
              var source = state;
              state = transfer(result, expected, state, call);
              var moved = state.moveFrom(source, result, context.site() + ":result");
              trackMovedObservers(moved);
              state = moved.state();
              result = moved.value();
              outputTransfers.putAll(moved.origins());
            }
          }
        }
        var restored = new HashMap<>(before.variables());
        for (String key : restored.keySet())
          if (!captures.containsKey(key))
            restored.put(key, state.variables().getOrDefault(key, restored.get(key)));
        outcomes.add(new Evaluation(state.withVariables(restored), result));
        targets.add(
            new ClosureInvocation.Target(
                identity,
                closure.expression(),
                captures,
                context.facts(),
                context.invocations(),
                context.mutations(),
                inputTransfers,
                outputTransfers));
      } finally {
        evaluations.pop();
        if (!evaluations.isEmpty()) evaluations.peek().mutations().addAll(context.mutations());
        activeClosures.remove(closure.expression());
      }
    }
    var invocation = new ClosureInvocation(targets);
    if (evaluations.isEmpty()) invocation.attach(call);
    else evaluations.peek().invocations().put(call, invocation);
    return outcomes.stream()
        .reduce((a, b) -> new Evaluation(a.state().join(b.state()), a.value().join(b.value())))
        .orElse(new Evaluation(before, Reference.nullValue()));
  }

  private static boolean scalar(CtTypeReference<?> type) {
    return type != null && type.isPrimitive();
  }

  private void error(String message, CtElement site) {
    diagnostics.add(Diagnostic.at(Diagnostic.Kind.PERMISSION, message, site));
  }

  private void problem(String message, CtElement site) {
    diagnostics.add(Diagnostic.at(Diagnostic.Kind.UNSUPPORTED, message, site));
  }
}
