package latte.refinement;

import static latte.refinement.JavaArithmetic.*;

import java.util.*;
import latte.analysis.*;
import latte.api.Diagnostic;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * A new refinement checker: Spoon supplies Java structure; Latte supplies ownership and aliases.
 */
public final class LiquidTypeChecking {
  private final List<Diagnostic> diagnostics = new ArrayList<>();
  private final Map<Term, String> referenceTypes = new HashMap<>();
  private final Map<Term, List<Term>> possibleAliases = new HashMap<>();
  private Contracts contracts;
  private PredicateResolver predicates;
  private Prover prover;
  private int counter;
  private final Deque<List<Completion>> exceptionCollectors = new ArrayDeque<>();
  private final Deque<Map<CtElement, LatteFacts>> ownershipFrames = new ArrayDeque<>();
  private final Deque<Map<CtElement, ClosureInvocation>> invocationFrames = new ArrayDeque<>();
  private final Deque<Reference> sharedCaptures = new ArrayDeque<>();

  private record ClosureEnvironment(Map<String, Term> values, Scope scope) {
    ClosureEnvironment {
      values = Map.copyOf(values);
    }
  }

  private final Map<String, ClosureEnvironment> closureEnvironments = new HashMap<>();

  private record Evaluation(TypingState state, Term value) {}

  private record Flow(List<Completion> paths) {
    Flow {
      paths = List.copyOf(paths);
    }

    Flow(TypingState state) {
      this(List.of(Completion.normal(state)));
    }
  }

  private record Location(
      TypingState state,
      Term value,
      String key,
      CtVariable<?> declaration,
      Term receiver,
      Term index) {}

  private record Scope(
      CtExecutable<?> executable,
      CtExecutable<?> contract,
      Map<String, CtVariable<?>> declarations,
      TypingState entry) {}

  public List<Diagnostic> analyze(CtModel model) {
    diagnostics.clear();
    referenceTypes.clear();
    possibleAliases.clear();
    ownershipFrames.clear();
    invocationFrames.clear();
    sharedCaptures.clear();
    closureEnvironments.clear();
    counter = 0;
    contracts = Contracts.collect(model);
    predicates = new PredicateResolver(contracts, this::referenceTypeOf, this::versionOf);
    diagnostics.addAll(contracts.diagnostics());
    try (var solver = new Prover(contracts.functions())) {
      prover = solver;
      for (var alias : contracts.aliases().values()) {
        var bindings = new HashMap<String, Term>();
        for (int i = 0; i < alias.parameters().size(); i++)
          bindings.put(
              alias.parameters().get(i), fresh(alias.parameters().get(i), alias.sorts().get(i)));
        try {
          prover.validate(
              predicates.resolve(
                  alias.predicate(),
                  bindings,
                  TypingState.empty(),
                  TypingState.empty(),
                  alias.site(),
                  0));
        } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
          report(Diagnostic.Kind.SYNTAX, failure.getMessage(), alias.site());
        }
      }
      model.getElements(new TypeFilter<>(CtExecutable.class)).stream()
          .filter(e -> e instanceof CtMethod<?> || e instanceof CtConstructor<?>)
          .filter(e -> !ExternalSpecifications.isLibrary(e))
          .forEach(
              e -> {
                try {
                  for (var contract : Contracts.sources(e)) executable(e, contract);
                } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
                  report(Diagnostic.Kind.SYNTAX, failure.getMessage(), e);
                }
              });
      for (var field : model.getElements(new TypeFilter<>(CtField.class)))
        if (Annotations.has(field, "Refinement")
            && eligible(field, field)
            && ExternalSpecifications.isSpecification(field)) {
          var receiver = reference(field, "external-field:" + Annotations.key(field));
          referenceTypes.put(receiver, ExternalSpecifications.owner(field.getDeclaringType()));
          var state = TypingState.empty().bind("this", receiver);
          var scope = new Scope(null, null, Map.of(), state);
          var value = fresh("field", sort(field.getType()));
          for (String text : Annotations.strings(field, "Refinement", "value"))
            try {
              prover.validate(
                  predicate(
                      text,
                      Map.of("_", value, field.getSimpleName(), value),
                      state,
                      state,
                      scope,
                      field));
            } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
              report(Diagnostic.Kind.SYNTAX, failure.getMessage(), field);
            }
        }
    }
    return diagnostics.stream().distinct().toList();
  }

  private void executable(CtExecutable<?> executable, CtExecutable<?> contract) {

    var declarations = new HashMap<String, CtVariable<?>>();
    executable.getParameters().forEach(p -> declarations.put(p.getSimpleName(), p));
    CtType<?> type = executable.getParent(CtType.class);
    var fields =
        type == null || ExternalSpecifications.isSpecification(executable)
            ? List.<CtField<?>>of()
            : executable instanceof CtConstructor<?> ? type.getFields() : visibleFields(type);
    fields.forEach(f -> declarations.put(f.getSimpleName(), f));
    for (int i = 0; i < executable.getParameters().size(); i++) {
      var parameter = executable.getParameters().get(i);
      declarations.put(parameter.getSimpleName(), parameter);
      if (i < contract.getParameters().size())
        declarations.put(contract.getParameters().get(i).getSimpleName(), parameter);
    }
    var scope = new Scope(executable, contract, Map.copyOf(declarations), null);
    var state = TypingState.empty();
    var receiver = reference(executable, "this:" + Annotations.key(executable));
    if (type != null) referenceTypes.put(receiver, ExternalSpecifications.owner(type));
    state =
        state.bind("this", receiver).version(receiverName(receiver), fresh("state", Term.Sort.REF));
    state = state.assume(Term.binary("!=", receiver, Term.NULL));
    state = stateAxioms(receiver, state);
    if (type != null) {
      // Java establishes defaults for every field before evaluating any instance initializer.
      for (var field : fields) {
        var value =
            executable instanceof CtConstructor<?> && !field.isStatic()
                ? defaultValue(field.getType())
                : fresh(field.getSimpleName(), sort(field.getType()));
        if (field.isFinal() && field.getDefaultExpression() instanceof CtLiteral<?> literal)
          value = literal(literal);
        if (field.isStatic() && field.isFinal())
          value =
              ConstantValues.resolve(
                      field.getReference().getQualifiedName().replace('#', '.'), field)
                  .orElse(value);
        state =
            registerObject(value, field.getType(), state)
                .bind(Annotations.key(field), value)
                .assume(bounds(value, field.getType()));
        if (!field.isStatic()) state = linkField(field, receiver, value, state);
      }
      for (var field : fields) {
        if (executable instanceof CtConstructor<?>
            && !field.isStatic()
            && field.getDefaultExpression() != null) {
          var evaluated = expression(field.getDefaultExpression(), state, scope);
          var value = convert(evaluated.value(), field.getType());
          var old = evaluated.state();
          state = advance(receiver, old).bind(Annotations.key(field), value);
          state = state.assume(frame(receiver, field.getSimpleName() + "()", state, old));
          state = linkField(field, receiver, value, state);
        }
        if (!(executable instanceof CtConstructor<?>)
            && Annotations.has(field, "Refinement")
            && eligible(field, field))
          state = assumeDeclared(field, state.values().get(Annotations.key(field)), state, scope);
      }
    }
    for (var parameter : executable.getParameters()) {
      var value = value(parameter, parameter.getType());
      state =
          state.bind(Annotations.key(parameter), value).assume(bounds(value, parameter.getType()));
      state = registerObject(value, parameter.getType(), state);
      int index = executable.getParameters().indexOf(parameter);
      var specified = contract.getParameters().get(index);
      if (Annotations.has(specified, "Refinement") && eligible(parameter, parameter))
        state = assumeDeclared(specified, value, state, scope);
    }
    if (executable instanceof CtMethod<?> method
        && Annotations.has(method, "Refinement")
        && sort(method.getType()) == Term.Sort.REF
        && !Annotations.permission(method).refinable())
      report(
          Diagnostic.Kind.SHARED_REFINEMENT,
          "A shared return value cannot carry a refinement; declare an ownership contract",
          method);
    var clauses = Annotations.named(contract, "StateRefinement");
    if (!clauses.isEmpty()) {
      Term precondition = Term.FALSE;
      for (var annotation : clauses) {
        String from = String.join("", Annotations.strings(annotation.getValues().get("from")));
        precondition =
            Term.or(
                precondition,
                from.isBlank()
                    ? Term.TRUE
                    : predicate(from, Map.of("this", receiver), state, state, scope, executable));
      }
      prover.validate(precondition);
      for (var annotation : clauses) {
        String to = String.join("", Annotations.strings(annotation.getValues().get("to")));
        var returned =
            executable instanceof CtMethod<?> m && !m.getType().getSimpleName().equals("void")
                ? fresh("return", sort(m.getType()))
                : receiver;
        if (!to.isBlank()) {
          try {
            prover.validate(
                predicate(
                    to,
                    Map.of("this", receiver, "_", returned, "return", returned),
                    state,
                    state,
                    scope,
                    executable));
          } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
            report(Diagnostic.Kind.SYNTAX, failure.getMessage(), annotation);
          }
        }
      }
      state = state.assume(precondition);
    }
    try {
      scope = new Scope(executable, contract, scope.declarations(), state);
      if (ExternalSpecifications.isSpecification(executable)) {
        var result =
            ExternalSpecifications.constructor(executable)
                ? receiver
                : fresh("result", sort(((CtMethod<?>) executable).getType()));
        for (String text : Annotations.strings(contract, "Refinement", "value"))
          prover.validate(
              predicate(
                  text, Map.of("_", result, "return", result), state, state, scope, contract));
        return;
      }
      if (executable == contract && executable instanceof CtMethod<?> method)
        checkOverride(method, scope);
      var completion = statement(executable.getBody(), state, scope);
      for (var path : completion.paths()) checkCompletion(path, scope);
    } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
      report(Diagnostic.Kind.SYNTAX, failure.getMessage(), executable);
    }
  }

  private Flow statement(CtStatement statement, TypingState before, Scope scope) {
    var flow = statementValue(statement, before, scope);
    if (statement != null && statement.getLabel() != null) {
      var label = statement.getLabel();
      return mergeFlow(
          flow.paths().stream()
              .map(
                  path ->
                      path.kind() == Completion.Kind.BREAK && label.equals(path.label())
                          ? Completion.normal(path.state())
                          : path)
              .toList());
    }
    return flow;
  }

  private Flow statementValue(CtStatement statement, TypingState before, Scope scope) {
    if (statement == null) return new Flow(before);
    if (statement instanceof CtBlock<?> block)
      return sequence(block.getStatements(), new Flow(before), scope);
    if (statement instanceof CtLocalVariable<?> local) {
      var evaluated =
          local.getDefaultExpression() == null
              ? new Evaluation(before, fresh(local.getSimpleName(), sort(local.getType())))
              : expression(local.getDefaultExpression(), before, scope);
      evaluated = new Evaluation(evaluated.state(), convert(evaluated.value(), local.getType()));
      var after =
          evaluated
              .state()
              .bind(Annotations.key(local), evaluated.value())
              .assume(aliasMembership(evaluated.value()));
      after = registerObject(evaluated.value(), local.getType(), after);
      if (Annotations.has(local, "Refinement")
          && eligible(local, local)
          && local.getDefaultExpression() != null)
        checkDeclared(local, evaluated.value(), after, scope, local);
      return new Flow(after);
    }
    if (statement instanceof CtIf conditional) {
      var guard = expression(conditional.getCondition(), before, scope);
      var yes =
          statement(conditional.getThenStatement(), guard.state().assume(guard.value()), scope);
      var no =
          statement(
              conditional.getElseStatement(), guard.state().assume(Term.not(guard.value())), scope);
      return mergeFlow(
          java.util.stream.Stream.concat(yes.paths().stream(), no.paths().stream()).toList());
    }
    if (statement instanceof CtReturn<?> returned) {
      var evaluated = expression(returned.getReturnedExpression(), before, scope);
      if (scope.executable() instanceof CtMethod<?> method)
        evaluated = new Evaluation(evaluated.state(), convert(evaluated.value(), method.getType()));
      return new Flow(
          List.of(
              new Completion(
                  Completion.Kind.RETURN,
                  evaluated.state(),
                  evaluated.value(),
                  null,
                  null,
                  returned)));
    }
    if (statement instanceof CtYieldStatement yielded) {
      var evaluated = expression(yielded.getExpression(), before, scope);
      return new Flow(
          List.of(
              new Completion(
                  Completion.Kind.YIELD,
                  evaluated.state(),
                  evaluated.value(),
                  null,
                  null,
                  yielded)));
    }
    if (statement instanceof CtLoop loop) return loop(loop, before, scope);
    if (statement instanceof CtThrow thrown) {
      var evaluated = expression(thrown.getThrownExpression(), before, scope);
      var type = thrown.getThrownExpression().getType();
      if (thrown.getThrownExpression() instanceof CtLiteral<?> literal
          && literal.getValue() == null)
        return new Flow(
            List.of(
                new Completion(
                    Completion.Kind.THROW,
                    evaluated.state(),
                    Term.NULL,
                    null,
                    "java.lang.NullPointerException",
                    thrown)));
      return new Flow(
          List.of(
              new Completion(
                  Completion.Kind.THROW,
                  evaluated.state(),
                  evaluated.value(),
                  null,
                  type == null ? "java.lang.Throwable" : type.getQualifiedName(),
                  thrown)));
    }
    if (statement instanceof CtBreak broken)
      return new Flow(
          List.of(
              new Completion(
                  Completion.Kind.BREAK,
                  before,
                  Term.TRUE,
                  broken.getTargetLabel(),
                  null,
                  broken)));
    if (statement instanceof CtContinue continued)
      return new Flow(
          List.of(
              new Completion(
                  Completion.Kind.CONTINUE,
                  before,
                  Term.TRUE,
                  continued.getTargetLabel(),
                  null,
                  continued)));
    if (statement instanceof CtTry tried) return tried(tried, before, scope);
    if (statement instanceof CtSwitch<?> switched)
      return switched(switched.getSelector(), switched.getCases(), before, scope, switched);
    if (statement instanceof CtSynchronized synchronizedBlock) {
      var monitor = expression(synchronizedBlock.getExpression(), before, scope);
      return statement(synchronizedBlock.getBlock(), monitor.state(), scope);
    }
    if (statement instanceof CtExpression<?> expression)
      return new Flow(expression(expression, before, scope).state());
    if (statement instanceof CtAssert<?> assertion) {
      var evaluated = expression(assertion.getAssertExpression(), before, scope);
      // Java assertions can be disabled; never assume their predicate afterwards.
      return new Flow(evaluated.state());
    }
    if (statement instanceof CtComment || statement instanceof CtType<?>) return new Flow(before);
    report(
        Diagnostic.Kind.UNSUPPORTED,
        "Refinement control flow is not implemented for " + statement.getClass().getSimpleName(),
        statement);
    return new Flow(havoc(before, scope));
  }

  private Flow sequence(List<CtStatement> statements, Flow initial, Scope scope) {
    var flow = initial;
    for (var child : statements) {
      var next = new ArrayList<Completion>();
      for (var path : flow.paths()) {
        if (!path.normal()) {
          next.add(path);
          continue;
        }
        try {
          next.addAll(statement(child, path.state(), scope).paths());
        } catch (IllegalArgumentException | com.microsoft.z3.Z3Exception failure) {
          report(Diagnostic.Kind.SYNTAX, failure.getMessage(), child);
          next.add(path);
        }
      }
      flow = mergeFlow(next);
    }
    return flow;
  }

  private Flow mergeFlow(List<Completion> paths) {
    var normal = paths.stream().filter(Completion::normal).map(Completion::state).toList();
    var result = new ArrayList<>(paths.stream().filter(p -> !p.normal()).toList());
    if (!normal.isEmpty()) result.add(Completion.normal(mergeStates(normal)));
    return new Flow(result);
  }

  private TypingState mergeStates(List<TypingState> states) {
    var merged = states.get(0);
    for (int i = 1; i < states.size(); i++) {
      var choose = fresh("path", Term.Sort.BOOL);
      merged =
          TypingState.join(choose, merged.assume(choose), states.get(i).assume(Term.not(choose)));
    }
    return merged;
  }

  private void checkCompletion(Completion path, Scope scope) {
    if (path.kind() != Completion.Kind.NORMAL && path.kind() != Completion.Kind.RETURN) return;
    var site = path.site() == null ? scope.executable() : path.site();
    if (path.kind() == Completion.Kind.RETURN)
      for (String text : Annotations.strings(scope.contract(), "Refinement", "value"))
        obligation(
            path.state(),
            predicate(text, Map.of("_", path.value()), path.state(), scope.entry(), scope, site),
            site,
            Diagnostic.Kind.REFINEMENT);
    for (String text : Annotations.strings(scope.executable(), "PrivateRefinement", "value")) {
      var bindings = new HashMap<String, Term>();
      if (path.value() != null) {
        bindings.put("_", path.value());
        bindings.put("return", path.value());
      }
      obligation(
          path.state(),
          predicate(text, bindings, path.state(), scope.entry(), scope, scope.executable()),
          site,
          Diagnostic.Kind.REFINEMENT);
    }
    if (scope.executable() instanceof CtConstructor<?>) checkFields(path.state(), scope, site);
    checkPreservedState(path.state(), scope, site);
    var receiver = path.state().values().get("this");
    var result = path.value() == null ? receiver : path.value();
    for (var clause : Annotations.named(scope.contract(), "StateRefinement")) {
      String from = String.join("", Annotations.strings(clause.getValues().get("from")));
      String to = String.join("", Annotations.strings(clause.getValues().get("to")));
      if (to.isBlank()) continue;
      var bindings = Map.of("this", receiver, "_", result, "return", result);
      var pre =
          from.isBlank()
              ? Term.TRUE
              : predicate(from, bindings, scope.entry(), scope.entry(), scope, scope.contract());
      var post =
          concretePart(
              predicate(to, bindings, path.state(), scope.entry(), scope, scope.contract()));
      if (!post.equals(Term.TRUE))
        obligation(path.state().assume(pre), post, site, Diagnostic.Kind.STATE);
    }
  }

  /**
   * Abstract state transitions are specifications; concrete field equations are body obligations.
   */
  private Term concretePart(Term term) {
    if (term instanceof Term.Binary binary && binary.operator().equals("&&"))
      return Term.and(concretePart(binary.left()), concretePart(binary.right()));
    return containsAbstractGhost(term) ? Term.TRUE : term;
  }

  private boolean containsAbstractGhost(Term term) {
    if (term instanceof Term.Call call)
      return (contracts.ghosts().contains(call.name())
              && !contracts.fields().containsKey(call.name()))
          || call.arguments().stream().anyMatch(this::containsAbstractGhost);
    if (term instanceof Term.Binary binary)
      return containsAbstractGhost(binary.left()) || containsAbstractGhost(binary.right());
    if (term instanceof Term.Unary unary) return containsAbstractGhost(unary.operand());
    if (term instanceof Term.Choice choice)
      return containsAbstractGhost(choice.condition())
          || containsAbstractGhost(choice.yes())
          || containsAbstractGhost(choice.no());
    return false;
  }

  private Flow loop(CtLoop loop, TypingState before, Scope scope) {
    var entry = before;
    if (loop instanceof CtFor f) {
      var initialized = sequence(f.getForInit(), new Flow(entry), scope);
      var normal =
          initialized.paths().stream().filter(Completion::normal).map(Completion::state).toList();
      if (normal.isEmpty()) return initialized;
      entry = mergeStates(normal);
    }
    var assigned = new LinkedHashSet<CtVariable<?>>();
    for (var write : loop.getElements(new TypeFilter<>(CtVariableWrite.class)))
      if (write.getVariable().getDeclaration() != null)
        assigned.add(write.getVariable().getDeclaration());
    for (var unary : loop.getElements(new TypeFilter<>(CtUnaryOperator.class)))
      if ((unary.getKind().name().contains("INC") || unary.getKind().name().contains("DEC"))
          && unary.getOperand() instanceof CtVariableAccess<?> access
          && access.getVariable().getDeclaration() != null)
        assigned.add(access.getVariable().getDeclaration());
    var arbitrary = entry;
    for (var variable : assigned) {
      var value = fresh(variable.getSimpleName(), sort(variable.getType()));
      arbitrary =
          registerObject(
                  value, variable.getType(), arbitrary.bind(Annotations.key(variable), value))
              .assume(bounds(value, variable.getType()));
      if (Annotations.has(variable, "Refinement") && eligible(variable, loop))
        arbitrary = assumeDeclared(variable, value, arbitrary, scope);
    }
    arbitrary = loopEffects(loop, arbitrary, scope);
    arbitrary = inferInvariants(loop, assigned, entry, arbitrary, scope);
    if (loop instanceof CtForEach each) {
      var iterable = expression(each.getExpression(), arbitrary, scope);
      var value = fresh(each.getVariable().getSimpleName(), sort(each.getVariable().getType()));
      arbitrary =
          iterable
              .state()
              .bind(Annotations.key(each.getVariable()), value)
              .assume(bounds(value, each.getVariable().getType()));
    }
    CtExpression<?> condition =
        loop instanceof CtWhile w
            ? w.getLoopingExpression()
            : loop instanceof CtDo d
                ? d.getLoopingExpression()
                : loop instanceof CtFor f ? f.getExpression() : null;
    var guard =
        condition == null || loop instanceof CtDo
            ? new Evaluation(arbitrary, Term.TRUE)
            : expression(condition, arbitrary, scope);
    var body =
        statement(
            loop.getBody(),
            loop instanceof CtDo ? arbitrary : guard.state().assume(guard.value()),
            scope);
    var exits = new ArrayList<Completion>();
    if (!(loop instanceof CtDo))
      exits.add(
          Completion.normal(
              loop instanceof CtForEach
                  ? arbitrary
                  : guard.state().assume(Term.not(guard.value()))));
    for (var path : body.paths()) {
      boolean targetsLoop = path.label() == null || Objects.equals(path.label(), loop.getLabel());
      if (path.kind() == Completion.Kind.BREAK && targetsLoop)
        exits.add(Completion.normal(path.state()));
      else if (path.normal() || path.kind() == Completion.Kind.CONTINUE && targetsLoop) {
        var tail = new Flow(path.state());
        if (loop instanceof CtFor f) tail = sequence(f.getForUpdate(), tail, scope);
        for (var updated : tail.paths()) {
          if (!updated.normal()) exits.add(updated);
          else if (loop instanceof CtDo) {
            var test = expression(condition, updated.state(), scope);
            exits.add(Completion.normal(test.state().assume(Term.not(test.value()))));
          }
        }
      } else exits.add(path);
    }
    return mergeFlow(exits);
  }

  private TypingState loopEffects(CtLoop loop, TypingState state, Scope scope) {
    var modified = new LinkedHashMap<Term, String>();
    for (CtInvocation<?> call : loop.getElements(new TypeFilter<>(CtInvocation.class))) {
      var callback = closureInvocation(call);
      if (callback.isPresent() && !callback.get().targets().isEmpty()) {
        for (var target : callback.get().targets())
          for (String identity : target.mutations()) modified.put(reference(call, identity), "*");
        continue;
      }
      var declaration = ExternalSpecifications.resolve(call.getExecutable());
      if (declaration == null) return havoc(state, scope);
      var effects = MutationEffects.on(declaration);
      if (effects.globals()) return havoc(state, scope);
      String post =
          String.join(
              " ", Annotations.strings(Contracts.source(declaration), "StateRefinement", "to"));
      if (effects.receiver() || !post.isBlank()) {
        var receiver = loopReference(call.getTarget(), state);
        if (receiver == null) return havoc(state, scope);
        modified.merge(receiver, post + modifiedFields(declaration), (a, b) -> a + " " + b);
      }
      for (int i = 0; i < call.getArguments().size(); i++)
        if (effects.parameter(i)) {
          var argument = loopReference(call.getArguments().get(i), state);
          if (argument == null) return havoc(state, scope);
          modified.put(argument, "*");
        }
    }
    for (CtConstructorCall<?> call : loop.getElements(new TypeFilter<>(CtConstructorCall.class))) {
      if (call.getType().getQualifiedName().equals("java.lang.Object")) continue;
      var effects = MutationEffects.on(ExternalSpecifications.resolve(call.getExecutable()));
      if (effects.globals()) return havoc(state, scope);
      for (int i = 0; i < call.getArguments().size(); i++)
        if (effects.parameter(i)) {
          var argument = loopReference(call.getArguments().get(i), state);
          if (argument == null) return havoc(state, scope);
          modified.put(argument, "*");
        }
    }
    for (CtFieldWrite<?> write : loop.getElements(new TypeFilter<>(CtFieldWrite.class))) {
      var receiver = loopReference(write.getTarget(), state);
      if (receiver != null && !write.getVariable().isStatic())
        modified.merge(receiver, write.getVariable().getSimpleName() + "()", (a, b) -> a + " " + b);
    }
    for (CtArrayWrite<?> write : loop.getElements(new TypeFilter<>(CtArrayWrite.class))) {
      var receiver = loopReference(write.getTarget(), state);
      if (receiver != null) modified.put(receiver, "*");
    }
    for (var change : modified.entrySet()) {
      var old = state;
      state = advance(change.getKey(), state);
      if (!change.getValue().equals("*") && !change.getValue().isBlank())
        state = state.assume(frame(change.getKey(), change.getValue(), state, old));
    }
    return havocFields(state, scope);
  }

  private Term loopReference(CtExpression<?> expression, TypingState state) {
    if (expression == null
        || expression instanceof CtThisAccess<?>
        || expression instanceof CtSuperAccess<?>) return state.values().get("this");
    if (expression instanceof CtVariableAccess<?> variable
        && variable.getVariable().getDeclaration() != null) {
      var declaration = variable.getVariable().getDeclaration();
      return state.values().get(Annotations.key(declaration));
    }
    return null;
  }

  private record Invariant(CtVariable<?> variable, String operator, Term bound) {
    Term at(TypingState state) {
      var value = state.values().get(Annotations.key(variable));
      return value == null ? Term.FALSE : Term.binary(operator, value, bound);
    }
  }

  /** Houdini-style elimination: an inferred bound must hold initially and on every back edge. */
  private TypingState inferInvariants(
      CtLoop loop,
      Set<CtVariable<?>> assigned,
      TypingState entry,
      TypingState arbitrary,
      Scope scope) {
    if (loop instanceof CtForEach || loop instanceof CtDo) return arbitrary;
    CtExpression<?> condition =
        loop instanceof CtWhile w
            ? w.getLoopingExpression()
            : loop instanceof CtFor f ? f.getExpression() : null;
    var candidates = new ArrayList<Invariant>();
    for (var variable : assigned) {
      if (variable instanceof CtField<?> || sort(variable.getType()) != Term.Sort.INT) continue;
      var initial = entry.values().get(Annotations.key(variable));
      if (initial == null) continue;
      var bounds = new LinkedHashSet<Term>(List.of(initial, Term.integer(0)));
      if (condition != null)
        for (CtBinaryOperator<?> comparison :
            condition.getElements(new TypeFilter<>(CtBinaryOperator.class))) {
          if (comparison.getLeftHandOperand() instanceof CtVariableAccess<?> access
              && access.getVariable().getDeclaration() == variable) {
            var right = comparison.getRightHandOperand();
            if (right instanceof CtLiteral<?> literal && literal.getValue() instanceof Number)
              bounds.add(literal(literal));
            else if (right instanceof CtVariableRead<?> read
                && read.getVariable().getDeclaration() != null
                && !assigned.contains(read.getVariable().getDeclaration())) {
              var value = entry.values().get(Annotations.key(read.getVariable().getDeclaration()));
              if (value != null) bounds.add(value);
            }
          }
        }
      for (var bound : bounds)
        for (String operator : List.of(">=", "<=", "==")) {
          var candidate = new Invariant(variable, operator, bound);
          if (prover.prove(entry.assumptions(), candidate.at(entry)).proven())
            candidates.add(candidate);
        }
    }
    if (candidates.isEmpty()) return arbitrary;
    int checkpoint = diagnostics.size();
    exceptionCollectors.push(new ArrayList<>());
    try {
      boolean changed;
      do {
        var state = arbitrary;
        for (var invariant : candidates) state = state.assume(invariant.at(state));
        var guard =
            condition == null
                ? new Evaluation(state, Term.TRUE)
                : expression(condition, state, scope);
        var paths = statement(loop.getBody(), guard.state().assume(guard.value()), scope).paths();
        var backEdges = new ArrayList<TypingState>();
        for (var path : paths)
          if (path.normal()
              || path.kind() == Completion.Kind.CONTINUE
                  && (path.label() == null || Objects.equals(path.label(), loop.getLabel()))) {
            var updated =
                loop instanceof CtFor f
                    ? sequence(f.getForUpdate(), new Flow(path.state()), scope)
                    : new Flow(path.state());
            updated.paths().stream()
                .filter(Completion::normal)
                .map(Completion::state)
                .forEach(backEdges::add);
          }
        changed =
            candidates.removeIf(
                candidate ->
                    backEdges.stream()
                        .anyMatch(
                            tail ->
                                !prover.prove(tail.assumptions(), candidate.at(tail)).proven()));
        diagnostics.subList(checkpoint, diagnostics.size()).clear();
      } while (changed && !candidates.isEmpty());
    } finally {
      exceptionCollectors.pop();
      diagnostics.subList(checkpoint, diagnostics.size()).clear();
    }
    var result = arbitrary;
    for (var invariant : candidates) result = result.assume(invariant.at(result));
    return result;
  }

  private Flow switched(
      CtExpression<?> selector,
      List<? extends CtCase<?>> cases,
      TypingState before,
      Scope scope,
      CtElement site) {
    var selected = expression(selector, before, scope);
    var tests = new ArrayList<Term>();
    Term any = Term.FALSE;
    for (var branch : cases) {
      Term test = Term.FALSE;
      for (var label : branch.getCaseExpressions())
        test =
            Term.or(
                test,
                Term.equal(selected.value(), expression(label, selected.state(), scope).value()));
      tests.add(test);
      any = Term.or(any, test);
    }
    var exits = new ArrayList<Completion>();
    boolean hasDefault = false;
    for (int i = 0; i < cases.size(); i++) {
      var branch = cases.get(i);
      boolean defaultCase = branch.getCaseExpressions().isEmpty() || branch.getIncludesDefault();
      hasDefault |= defaultCase;
      var test = defaultCase ? Term.or(tests.get(i), Term.not(any)) : tests.get(i);
      var flow = new Flow(selected.state().assume(test));
      for (int j = i; j < cases.size(); j++) {
        flow = sequence(cases.get(j).getStatements(), flow, scope);
        if (cases.get(j).getCaseKind() == CaseKind.ARROW) break;
      }
      for (var path : flow.paths())
        exits.add(
            path.kind() == Completion.Kind.BREAK && path.label() == null
                ? Completion.normal(path.state())
                : path);
    }
    if (!hasDefault) exits.add(Completion.normal(selected.state().assume(Term.not(any))));
    return mergeFlow(exits);
  }

  private Flow tried(CtTry tried, TypingState before, Scope scope) {
    var thrown = new ArrayList<Completion>();
    exceptionCollectors.push(thrown);
    Flow body;
    try {
      var initialized = new Flow(before);
      if (tried instanceof CtTryWithResource resources)
        for (var resource : resources.getResources())
          if (resource instanceof CtStatement statement)
            initialized = sequence(List.of(statement), initialized, scope);
      var paths = new ArrayList<Completion>();
      for (var path : initialized.paths())
        paths.addAll(
            path.normal()
                ? statement(tried.getBody(), path.state(), scope).paths()
                : List.of(path));
      paths.addAll(thrown);
      body = new Flow(paths);
    } finally {
      exceptionCollectors.pop();
    }
    if (tried instanceof CtTryWithResource resources) body = closeResources(resources, body, scope);
    var handled = new ArrayList<Completion>();
    var handlerThrows = new ArrayList<Completion>();
    exceptionCollectors.push(handlerThrows);
    try {
      for (var path : body.paths()) {
        var pending = List.of(path);
        for (var catcher : tried.getCatchers()) {
          var remaining = new ArrayList<Completion>();
          for (var candidate : pending) {
            if (candidate.kind() != Completion.Kind.THROW) {
              remaining.add(candidate);
              continue;
            }
            boolean covers = false, possible = false;
            for (var caught : catcher.getParameter().getMultiTypes()) {
              covers |= subtype(candidate.exceptionType(), caught.getQualifiedName(), tried);
              possible |= subtype(caught.getQualifiedName(), candidate.exceptionType(), tried);
            }
            if (!covers && !possible) {
              remaining.add(candidate);
              continue;
            }
            var caughtState = candidate.state();
            if (!covers) {
              var choice = fresh("caught", Term.Sort.BOOL);
              caughtState = caughtState.assume(choice);
              remaining.add(candidate.withState(candidate.state().assume(Term.not(choice))));
            }
            var exception = fresh("exception", Term.Sort.REF);
            caughtState =
                caughtState
                    .bind(Annotations.key(catcher.getParameter()), exception)
                    .assume(Term.binary("!=", exception, Term.NULL));
            handled.addAll(statement(catcher.getBody(), caughtState, scope).paths());
          }
          pending = remaining;
        }
        handled.addAll(pending);
      }
    } finally {
      exceptionCollectors.pop();
    }
    handled.addAll(handlerThrows);
    if (tried.getFinalizer() == null) return mergeFlow(handled);
    var finalized = new ArrayList<Completion>();
    for (var path : handled)
      for (var outcome : statement(tried.getFinalizer(), path.state(), scope).paths())
        finalized.add(outcome.normal() ? path.withState(outcome.state()) : outcome);
    return mergeFlow(finalized);
  }

  private boolean subtype(String subtype, String supertype, CtElement site) {
    if (subtype == null) return false;
    if (subtype.equals(supertype) || supertype.equals("java.lang.Throwable")) return true;
    try {
      return site.getFactory()
          .Type()
          .createReference(subtype)
          .isSubtypeOf(site.getFactory().Type().createReference(supertype));
    } catch (RuntimeException unavailable) {
      return false;
    }
  }

  private void possibleThrow(String type, TypingState state, CtElement site) {
    if (!exceptionCollectors.isEmpty())
      exceptionCollectors
          .peek()
          .add(new Completion(Completion.Kind.THROW, state, Term.NULL, null, type, site));
  }

  private Flow closeResources(CtTryWithResource tried, Flow body, Scope scope) {
    var flow = body;
    for (int i = tried.getResources().size() - 1; i >= 0; i--) {
      var resource = tried.getResources().get(i);
      CtVariable<?> variable =
          resource instanceof CtVariable<?> v
              ? v
              : resource instanceof CtVariableAccess<?> a ? a.getVariable().getDeclaration() : null;
      if (variable == null) continue;
      var next = new ArrayList<Completion>();
      for (var path : flow.paths()) {
        var value = path.state().values().get(Annotations.key(variable));
        if (value == null) {
          next.add(path);
          continue;
        }
        var factory = tried.getFactory();
        var read = factory.Code().createVariableRead(variable.getReference(), false);
        read.setPosition(tried.getPosition());
        read.setParent(tried);
        facts(variable).ifPresent(facts -> facts.attach(read));
        var reference = factory.Core().<Object>createExecutableReference();
        reference.setSimpleName("close");
        reference.setDeclaringType(variable.getType());
        reference.setType(factory.Type().createReference(void.class));
        var close = factory.Code().createInvocation(read, reference);
        close.setPosition(tried.getPosition());
        close.setParent(tried);
        var exceptions = new ArrayList<Completion>();
        exceptionCollectors.push(exceptions);
        Evaluation result;
        try {
          result =
              call(
                  reference,
                  List.of(),
                  read,
                  close,
                  path.state().assume(Term.binary("!=", value, Term.NULL)),
                  scope);
        } finally {
          exceptionCollectors.pop();
        }
        next.add(path.withState(result.state()));
        next.add(path.withState(path.state().assume(Term.equal(value, Term.NULL))));
        for (var exception : exceptions)
          next.add(
              path.kind() == Completion.Kind.THROW ? path.withState(exception.state()) : exception);
      }
      flow = new Flow(next);
    }
    return mergeFlow(flow.paths());
  }

  private Evaluation expression(CtExpression<?> expression, TypingState before, Scope scope) {
    var result = expressionValue(expression, before, scope);
    if (expression == null) return result;
    var value = cast(result.value(), expression);
    var state = result.state();
    var unboxed = JavaTypes.unboxed(expression.getType());
    var expected = JavaTypes.expected(expression);
    if (unboxed.isPresent() && expected != null && Contracts.sort(expected) != Term.Sort.REF) {
      possibleThrow(
          "java.lang.NullPointerException",
          state.assume(Term.equal(result.value(), Term.NULL)),
          expression);
      state = state.assume(Term.binary("!=", result.value(), Term.NULL));
      value = new Term.Call("unbox:" + unboxed.get(), List.of(value));
    }
    return new Evaluation(state, value);
  }

  private Evaluation expressionValue(CtExpression<?> expression, TypingState before, Scope scope) {
    if (expression == null) return new Evaluation(before, Term.TRUE);
    if (expression instanceof CtLiteral<?> literal) return new Evaluation(before, literal(literal));
    if (expression instanceof CtThisAccess<?> || expression instanceof CtSuperAccess<?>)
      return new Evaluation(before, before.values().get("this"));
    if (expression instanceof CtFieldRead<?> field) {
      var declaration = ExternalSpecifications.resolve(field.getVariable());
      if (field.getVariable().getSimpleName().equals("length")
          && field.getTarget() != null
          && field.getTarget().getType() != null
          && field.getTarget().getType().isArray()) {
        var receiver = expression(field.getTarget(), before, scope);
        return new Evaluation(
            nonnull(receiver.value(), receiver.state(), field),
            new Term.Call("length", List.of(receiver.value())));
      }
      var constant =
          ConstantValues.resolve(field.getVariable().getQualifiedName().replace('#', '.'), field)
              .orElse(null);
      if (constant != null) return new Evaluation(before, constant);
      if (declaration instanceof CtEnumValue<?> enumValue)
        return new Evaluation(
            before,
            new Term.Symbol(
                "enum:"
                    + enumValue.getDeclaringType().getQualifiedName()
                    + "."
                    + enumValue.getSimpleName(),
                Term.Sort.REF));
      var receiver = expression(field.getTarget(), before, scope);
      if (!field.getVariable().isStatic())
        receiver =
            new Evaluation(nonnull(receiver.value(), receiver.state(), field), receiver.value());
      Term fieldValue = null;
      if (declaration != null && field.getVariable().isStatic())
        fieldValue = receiver.state().values().get(Annotations.key(declaration));
      else if (declaration != null)
        fieldValue = fieldValue(declaration, receiver.value(), receiver.state());
      if (fieldValue == null) fieldValue = fresh("field", sort(field.getType()));
      var after = receiver.state().assume(bounds(fieldValue, field.getType()));
      if (ExternalSpecifications.isSpecification(declaration)
          && Annotations.has(declaration, "Refinement")
          && eligible(declaration, field)) {
        for (String text : Annotations.strings(declaration, "Refinement", "value"))
          after =
              after.assume(
                  predicate(
                      text,
                      Map.of(
                          "_",
                          fieldValue,
                          declaration.getSimpleName(),
                          fieldValue,
                          "this",
                          receiver.value()),
                      after,
                      after,
                      scope,
                      declaration));
      }
      if (sort(field.getType()) == Term.Sort.REF) {
        var referenceValue = value(field, field.getType());
        after =
            registerObject(referenceValue, field.getType(), after)
                .assume(aliasMembership(referenceValue))
                .assume(Term.equal(referenceValue, fieldValue));
        return new Evaluation(after, referenceValue);
      }
      return new Evaluation(after, fieldValue);
    }
    if (expression instanceof CtVariableAccess<?> read) {
      var declaration = read.getVariable().getDeclaration();
      var value = declaration == null ? null : before.values().get(Annotations.key(declaration));
      if (declaration != null && Annotations.has(declaration, "Refinement"))
        eligible(declaration, read);
      if (value == null) value = fresh(read.getVariable().getSimpleName(), sort(read.getType()));
      return new Evaluation(before.assume(bounds(value, read.getType())), value);
    }
    if (expression instanceof CtAssignment<?, ?> assignment) {
      var targetBefore = location(assignment.getAssigned(), before, scope);
      var evaluated = expression(assignment.getAssignment(), targetBefore.state(), scope);
      Term result = evaluated.value();
      if (assignment instanceof CtOperatorAssignment<?, ?> operator) {
        var old = targetBefore;
        if ((operator.getKind() == BinaryOperatorKind.DIV
                || operator.getKind() == BinaryOperatorKind.MOD)
            && sort(assignment.getType()) == Term.Sort.INT) {
          var zero = Term.equal(result, Term.integer(0));
          possibleThrow(
              "java.lang.ArithmeticException", evaluated.state().assume(zero), assignment);
          evaluated = new Evaluation(evaluated.state().assume(Term.not(zero)), evaluated.value());
        }
        result = binary(operator.getKind(), old.value(), result, assignment.getType());
      }
      result = convert(result, assignment.getAssigned().getType());
      var after = write(targetBefore, result, evaluated.state(), scope, assignment);
      return new Evaluation(after, result);
    }
    if (expression instanceof CtBinaryOperator<?> binary) {
      var left = expression(binary.getLeftHandOperand(), before, scope);
      if (binary.getKind() == BinaryOperatorKind.INSTANCEOF) {
        var tested =
            binary.getRightHandOperand() instanceof CtTypeAccess<?> type
                ? type.getAccessedType()
                : binary.getRightHandOperand().getType();
        var actual = binary.getLeftHandOperand().getType();
        Term matches =
            new Term.Call("instanceof:" + tested.getQualifiedName(), List.of(left.value()));
        if (actual != null && actual.isSubtypeOf(tested)) matches = Term.TRUE;
        return new Evaluation(
            left.state(), Term.and(Term.binary("!=", left.value(), Term.NULL), matches));
      }
      boolean shortCircuit =
          binary.getKind() == BinaryOperatorKind.AND || binary.getKind() == BinaryOperatorKind.OR;
      var evaluatedRight =
          shortCircuit
              ? left.state()
                  .assume(
                      binary.getKind() == BinaryOperatorKind.AND
                          ? left.value()
                          : Term.not(left.value()))
              : left.state();
      var right = expression(binary.getRightHandOperand(), evaluatedRight, scope);
      if ((binary.getKind() == BinaryOperatorKind.DIV || binary.getKind() == BinaryOperatorKind.MOD)
          && sort(binary.getType()) == Term.Sort.INT) {
        var zero = Term.equal(right.value(), Term.integer(0));
        possibleThrow("java.lang.ArithmeticException", right.state().assume(zero), binary);
        right = new Evaluation(right.state().assume(Term.not(zero)), right.value());
      }
      var state =
          shortCircuit
              ? TypingState.join(
                  binary.getKind() == BinaryOperatorKind.AND
                      ? left.value()
                      : Term.not(left.value()),
                  right.state(),
                  left.state())
              : right.state();
      if (binary.getKind() == BinaryOperatorKind.PLUS
          && ("java.lang.String".equals(String.valueOf(binary.getLeftHandOperand().getType()))
              || "java.lang.String".equals(String.valueOf(binary.getRightHandOperand().getType()))))
        return new Evaluation(state, fresh("concatenation", Term.Sort.REF));
      return new Evaluation(
          state, binary(binary.getKind(), left.value(), right.value(), JavaTypes.result(binary)));
    }
    if (expression instanceof CtUnaryOperator<?> unary) {
      String kind = unary.getKind().name();
      if (kind.contains("INC") || kind.contains("DEC")) {
        var evaluated = location(unary.getOperand(), before, scope);
        var next =
            wrap(
                Term.binary(kind.contains("INC") ? "+" : "-", evaluated.value(), Term.integer(1)),
                unary.getType());
        var after = write(evaluated, next, evaluated.state(), scope, unary);
        return new Evaluation(after, kind.startsWith("POST") ? evaluated.value() : next);
      }
      var evaluated = expression(unary.getOperand(), before, scope);
      var value =
          switch (unary.getKind()) {
            case NOT -> Term.not(evaluated.value());
            case NEG -> wrap(new Term.Unary("-", evaluated.value()), unary.getType());
            case POS -> evaluated.value();
            case COMPL -> wrap(new Term.Unary("~", evaluated.value()), unary.getType());
            default -> throw new IllegalArgumentException("Unsupported unary operator");
          };
      return new Evaluation(evaluated.state(), value);
    }
    if (expression instanceof CtSwitchExpression<?, ?> switched) {
      var flow = switched(switched.getSelector(), switched.getCases(), before, scope, switched);
      var result = fresh("switchResult", sort(switched.getType()));
      var outcomes = new ArrayList<TypingState>();
      for (var path : flow.paths()) {
        if (path.kind() == Completion.Kind.THROW)
          possibleThrow(path.exceptionType(), path.state(), switched);
        else if (path.kind() == Completion.Kind.YIELD)
          outcomes.add(path.state().bind(result.name(), convert(path.value(), switched.getType())));
      }
      if (outcomes.isEmpty()) return new Evaluation(before.assume(Term.FALSE), result);
      var merged = mergeStates(outcomes);
      return new Evaluation(merged, merged.values().get(result.name()));
    }
    if (expression instanceof CtConditional<?> conditional) {
      var guard = expression(conditional.getCondition(), before, scope);
      var yes =
          expression(conditional.getThenExpression(), guard.state().assume(guard.value()), scope);
      var no =
          expression(
              conditional.getElseExpression(),
              guard.state().assume(Term.not(guard.value())),
              scope);
      return new Evaluation(
          TypingState.join(guard.value(), yes.state(), no.state()),
          new Term.Choice(guard.value(), yes.value(), no.value()));
    }
    if (expression instanceof CtConstructorCall<?> constructor)
      return call(
          constructor.getExecutable(),
          constructor.getArguments(),
          null,
          constructor,
          before,
          scope);
    if (expression instanceof CtInvocation<?> invocation)
      return call(
          invocation.getExecutable(),
          invocation.getArguments(),
          invocation.getTarget(),
          invocation,
          before,
          scope);
    if (expression instanceof CtNewArray<?> array) {
      var state = before;
      Term length = Term.integer(array.getElements().size());
      var dimensions = new ArrayList<Term>();
      for (int i = 0; i < array.getDimensionExpressions().size(); i++) {
        var evaluated = expression(array.getDimensionExpressions().get(i), state, scope);
        state = evaluated.state();
        dimensions.add(evaluated.value());
        if (i == 0) length = evaluated.value();
      }
      for (var dimension : dimensions) {
        var invalid = Term.binary("<", dimension, Term.integer(0));
        possibleThrow("java.lang.NegativeArraySizeException", state.assume(invalid), array);
        state = state.assume(Term.not(invalid));
      }
      var value = value(array, array.getType());
      state = allocateObject(array, value, array.getType(), state);
      var negative = Term.binary("<", length, Term.integer(0));
      possibleThrow("java.lang.NegativeArraySizeException", state.assume(negative), array);
      state =
          state
              .assume(Term.not(negative))
              .assume(Term.binary("!=", value, Term.NULL))
              .assume(Term.equal(new Term.Call("length", List.of(value)), length));
      var component = ((CtArrayTypeReference<?>) array.getType()).getComponentType();
      Term contents =
          array.getDimensionExpressions().size() > 1
              ? new Term.ArraySymbol("nestedArray$" + counter++, Term.Sort.REF)
              : new Term.ArrayConstant(defaultValue(component));
      for (int i = 0; i < array.getElements().size(); i++) {
        var element = expression(array.getElements().get(i), state, scope);
        state = element.state();
        contents =
            new Term.ArrayStore(contents, Term.integer(i), convert(element.value(), component));
      }
      state = storeArray(value, contents, state);
      return new Evaluation(state, value);
    }
    if (expression instanceof CtArrayRead<?> array) {
      var receiver = expression(array.getTarget(), before, scope);
      var index = expression(array.getIndexExpression(), receiver.state(), scope);
      var state = arrayBounds(receiver.value(), index.value(), index.state(), array);
      var element =
          new Term.ArraySelect(
              arrayContents(receiver.value(), sort(array.getType()), state), index.value());
      if (sort(array.getType()) == Term.Sort.REF) {
        var identity = value(array, array.getType());
        state =
            registerObject(identity, array.getType(), state)
                .assume(aliasMembership(identity))
                .assume(Term.equal(identity, element));
        return new Evaluation(state, identity);
      }
      return new Evaluation(state.assume(bounds(element, array.getType())), element);
    }
    if (expression instanceof CtTypeAccess<?>) return new Evaluation(before, Term.NULL);
    if (expression instanceof CtLambda<?> lambda) {
      rememberClosure(lambda, before, scope, null);
      checkClosure(lambda, before, scope);
      return new Evaluation(before, value(expression, expression.getType()));
    }
    if (expression instanceof CtExecutableReferenceExpression<?, ?> reference) {
      var evaluated =
          reference.getTarget() instanceof CtTypeAccess<?>
              ? new Evaluation(before, Term.NULL)
              : expression(reference.getTarget(), before, scope);
      rememberClosure(reference, evaluated.state(), scope, evaluated.value());
      checkClosure(reference, evaluated.state(), scope);
      return new Evaluation(evaluated.state(), value(expression, expression.getType()));
    }
    report(
        Diagnostic.Kind.UNSUPPORTED,
        "Refinement expression is not implemented for " + expression.getClass().getSimpleName(),
        expression);
    return new Evaluation(havocObjects(before), fresh("unsupported", sort(expression.getType())));
  }

  private void deferred(CtLambda<?> lambda, TypingState captured, Scope outer) {
    var contract = lambda.getOverriddenMethod();
    CtExecutable<?> specification = contract == null ? lambda : contract;
    var declarations = new HashMap<>(outer.declarations());
    var closure = ClosureFacts.on(lambda).orElse(new ClosureFacts(Set.of(), true, false));
    var state = closure.deferred() ? havoc(captured, outer) : captured;
    for (int i = 0; i < lambda.getParameters().size(); i++) {
      var parameter = lambda.getParameters().get(i);
      var parameterValue = value(parameter, parameter.getType());
      state =
          registerObject(
                  parameterValue,
                  parameter.getType(),
                  state.bind(Annotations.key(parameter), parameterValue))
              .assume(bounds(parameterValue, parameter.getType()));
      declarations.put(parameter.getSimpleName(), parameter);
      if (i < specification.getParameters().size())
        declarations.put(specification.getParameters().get(i).getSimpleName(), parameter);
    }
    var scope = new Scope(lambda, specification, Map.copyOf(declarations), state);
    for (int i = 0; i < lambda.getParameters().size(); i++) {
      var parameter = lambda.getParameters().get(i);
      var specified = specification.getParameters().get(i);
      if (Annotations.has(specified, "Refinement") && eligible(parameter, parameter))
        state =
            assumeDeclared(specified, state.values().get(Annotations.key(parameter)), state, scope);
    }
    scope = new Scope(lambda, specification, scope.declarations(), state);
    // Exceptions belong to the eventual invocation, never to a try enclosing closure creation.
    exceptionCollectors.push(new ArrayList<>());
    try {
      if (lambda.getBody() != null)
        for (var path : statement(lambda.getBody(), state, scope).paths())
          checkCompletion(path, scope);
      else {
        var evaluated = expression(lambda.getExpression(), state, scope);
        checkCompletion(
            new Completion(
                Completion.Kind.RETURN, evaluated.state(), evaluated.value(), null, null, lambda),
            scope);
      }
    } finally {
      exceptionCollectors.pop();
    }
  }

  private Optional<LatteFacts> facts(CtElement node) {
    if (node == null) return Optional.empty();
    LatteFacts found = null;
    for (var frame : ownershipFrames)
      if (frame.containsKey(node)) {
        found = frame.get(node);
        break;
      }
    var result = found == null ? LatteFacts.on(node) : Optional.of(found);
    if (sharedCaptures.isEmpty()) return result;
    var captures = sharedCaptures.peek();
    return result.map(
        f ->
            new LatteFacts(
                f.before().changeReachable(captures, Permission.SHARED),
                f.after().changeReachable(captures, Permission.SHARED),
                f.value()));
  }

  private void rememberClosure(
      CtExpression<?> expression, TypingState state, Scope scope, Term receiver) {
    var values = new HashMap<String, Term>();
    for (String key : ClosureFacts.on(expression).orElseThrow().bindings()) {
      var captured = key.equals(ClosureFacts.RECEIVER) ? receiver : state.values().get(key);
      if (captured != null) values.put(key, captured);
    }
    for (String identity : facts(expression).orElseThrow().value().objects())
      closureEnvironments.put(identity, new ClosureEnvironment(values, scope));
  }

  private Optional<ClosureInvocation> closureInvocation(CtElement call) {
    for (var frame : invocationFrames)
      if (frame.containsKey(call)) return Optional.of(frame.get(call));
    return ClosureInvocation.on(call);
  }

  private void checkClosure(CtExpression<?> expression, TypingState state, Scope scope) {
    var closure = ClosureFacts.on(expression).orElseThrow();
    var captures = Reference.nullValue();
    if (closure.escaped()) {
      var ownership = facts(expression).orElseThrow();
      for (String key : closure.bindings())
        captures =
            captures.join(
                ownership
                    .after()
                    .read(ownership.value(), ClosureFacts.CAPTURE + key)
                    .orElse(Reference.nullValue()));
    }
    sharedCaptures.push(captures);
    try {
      if (expression instanceof CtLambda<?> lambda) deferred(lambda, state, scope);
      else if (expression instanceof CtExecutableReferenceExpression<?, ?> reference)
        methodReference(reference, state, scope);
    } finally {
      sharedCaptures.pop();
    }
  }

  private Evaluation invokeClosure(
      ClosureInvocation invocation,
      Term receiver,
      List<Term> values,
      List<CtExpression<?>> arguments,
      CtExpression<?> call,
      TypingState before,
      Scope outer) {
    var outcomes = new ArrayList<TypingState>();
    var result =
        sort(JavaTypes.result(call)) == Term.Sort.REF
            ? value(call, JavaTypes.result(call))
            : fresh("callbackResult", sort(JavaTypes.result(call)));
    for (var target : invocation.targets()) {
      var environment = closureEnvironments.get(target.identity());
      var lexical = environment == null ? outer : environment.scope();
      var declarations = new HashMap<>(lexical.declarations());
      for (var read : target.expression().getElements(new TypeFilter<>(CtVariableRead.class))) {
        var declaration = read.getVariable().getDeclaration();
        if (declaration != null) declarations.put(declaration.getSimpleName(), declaration);
      }
      var state = before.assume(Term.equal(receiver, reference(call, target.identity())));
      state = transferVersions(call, target.inputTransfers(), state);
      for (var entry : target.captures().entrySet()) {
        var captured = environment == null ? null : environment.values().get(entry.getKey());
        if (!entry.getValue().scalar()) {
          var identities =
              entry.getValue().objects().stream().map(id -> reference(call, id)).toList();
          if (identities.isEmpty()) captured = Term.NULL;
          else if (identities.size() == 1) captured = identities.get(0);
          else {
            captured = fresh("capture", Term.Sort.REF);
            possibleAliases.put(captured, identities);
            state = state.assume(aliasMembership(captured));
          }
        }
        if (captured == null) captured = state.values().get(entry.getKey());
        var declaration =
            declarations.values().stream()
                .filter(d -> Annotations.key(d).equals(entry.getKey()))
                .findFirst()
                .orElse(null);
        if (captured == null)
          captured =
              fresh("capture", declaration == null ? Term.Sort.REF : sort(declaration.getType()));
        if (declaration != null) state = registerObject(captured, declaration.getType(), state);
        state = state.bind(entry.getKey(), captured);
      }
      var functional = ClosureFacts.functional(target.expression());
      ownershipFrames.push(target.facts());
      invocationFrames.push(target.invocations());
      try {
        if (functional != null) {
          var pre = contractPrecondition(functional, values, receiver, state, outer);
          obligation(state, pre, call, Diagnostic.Kind.REFINEMENT);
          state = state.assume(pre);
        }
        List<Completion> paths;
        if (target.expression() instanceof CtLambda<?> lambda) {
          for (int i = 0; i < Math.min(values.size(), lambda.getParameters().size()); i++) {
            var parameter = lambda.getParameters().get(i);
            var parameterValue = values.get(i);
            if (Annotations.permission(functional.getParameters().get(i)) == Permission.FREE
                && sort(parameter.getType()) == Term.Sort.REF) {
              parameterValue = value(parameter, parameter.getType());
              state = state.assume(Term.equal(parameterValue, values.get(i)));
            }
            state = state.bind(Annotations.key(parameter), parameterValue);
            declarations.put(parameter.getSimpleName(), parameter);
            if (functional != null)
              declarations.put(functional.getParameters().get(i).getSimpleName(), parameter);
          }
          var scope =
              new Scope(
                  lambda,
                  functional == null ? lambda : functional,
                  Map.copyOf(declarations),
                  state);
          if (lambda.getBody() != null) paths = statement(lambda.getBody(), state, scope).paths();
          else {
            var evaluated = expression(lambda.getExpression(), state, scope);
            paths =
                List.of(
                    new Completion(
                        Completion.Kind.RETURN,
                        evaluated.state(),
                        evaluated.value(),
                        null,
                        null,
                        lambda));
          }
          for (var path : paths) checkCompletion(path, scope);
        } else {
          var reference = (CtExecutableReferenceExpression<?, ?>) target.expression();
          boolean unbound =
              reference.getTarget() instanceof CtTypeAccess<?>
                  && !reference.getExecutable().isStatic()
                  && !reference.getExecutable().isConstructor();
          var bound =
              unbound && !values.isEmpty()
                  ? values.get(0)
                  : state.values().getOrDefault(ClosureFacts.RECEIVER, Term.NULL);
          var actual = unbound ? values.subList(1, values.size()) : values;
          var nodes = unbound ? arguments.subList(1, arguments.size()) : arguments;
          var declaration =
              Contracts.source(ExternalSpecifications.resolve(reference.getExecutable()));
          if (reference.getExecutable().isConstructor()) {
            bound = result;
            state =
                allocateObject(call, result, reference.getExecutable().getDeclaringType(), state);
          }
          Evaluation evaluated;
          if (declaration == null) evaluated = new Evaluation(havoc(state, outer), result);
          else
            evaluated =
                applyContract(
                    declaration,
                    new ArrayList<>(actual),
                    new Evaluation(state, bound),
                    result,
                    reference.getExecutable().isConstructor(),
                    nodes,
                    reference.getTarget(),
                    call,
                    state,
                    outer);
          paths =
              List.of(
                  new Completion(
                      Completion.Kind.RETURN,
                      evaluated.state(),
                      evaluated.value(),
                      null,
                      null,
                      call));
        }
        for (var path : paths) {
          if (path.kind() == Completion.Kind.THROW) {
            possibleThrow(path.exceptionType(), path.state(), call);
            continue;
          }
          if (!path.normal() && path.kind() != Completion.Kind.RETURN) continue;
          var after = transferVersions(call, target.outputTransfers(), path.state());
          if (path.kind() == Completion.Kind.RETURN
              && !JavaTypes.result(call).getSimpleName().equals("void"))
            after = after.assume(Term.equal(result, path.value()));
          var restored = new HashMap<>(after.values());
          for (String key : target.captures().keySet()) {
            if (before.values().containsKey(key)) restored.put(key, before.values().get(key));
            else restored.remove(key);
          }
          outcomes.add(new TypingState(restored, after.versions(), after.assumptions()));
        }
      } finally {
        ownershipFrames.pop();
        invocationFrames.pop();
      }
    }
    return new Evaluation(
        outcomes.isEmpty() ? before.assume(Term.FALSE) : mergeStates(outcomes), result);
  }

  private void methodReference(
      CtExecutableReferenceExpression<?, ?> reference, TypingState captured, Scope scope) {
    var target = ExternalSpecifications.resolve(reference.getExecutable());
    var type = reference.getType() == null ? null : reference.getType().getTypeDeclaration();
    if (target == null || type == null) return;
    var functional =
        type.getAllMethods().stream()
            .filter(
                m ->
                    m.isAbstract()
                        && !m.isStatic()
                        && !m.getDeclaringType().getQualifiedName().equals("java.lang.Object"))
            .findFirst()
            .orElse(null);
    if (functional == null) return;
    var closure = ClosureFacts.on(reference).orElse(new ClosureFacts(Set.of(), true, false));
    var state = closure.deferred() ? havoc(captured, scope) : captured;
    if (closure.escaped()
        && !Annotations.named(target, "StateRefinement").isEmpty()
        && !(reference.getTarget() instanceof CtTypeAccess<?>))
      report(
          Diagnostic.Kind.SHARED_REFINEMENT,
          "A shared captured receiver cannot carry state refinements",
          reference);
    var values = new ArrayList<Term>();
    for (var parameter : functional.getParameters()) {
      var value = fresh(parameter.getSimpleName(), sort(parameter.getType()));
      values.add(value);
      state =
          registerObject(value, parameter.getType(), state)
              .assume(bounds(value, parameter.getType()));
    }
    boolean unbound =
        reference.getTarget() instanceof CtTypeAccess<?>
            && !reference.getExecutable().isStatic()
            && !reference.getExecutable().isConstructor();
    var savedReceiver =
        facts(reference).stream()
            .flatMap(f -> f.value().objects().stream())
            .map(closureEnvironments::get)
            .filter(Objects::nonNull)
            .map(e -> e.values().get(ClosureFacts.RECEIVER))
            .filter(Objects::nonNull)
            .findFirst();
    var receiver =
        unbound && !values.isEmpty()
            ? values.get(0)
            : savedReceiver.isPresent()
                ? savedReceiver.get()
                : expression(reference.getTarget(), state, scope).value();
    if (reference.getExecutable().isConstructor()) {
      receiver = fresh("constructed", Term.Sort.REF);
      state =
          registerObject(receiver, reference.getExecutable().getDeclaringType(), state)
              .assume(Term.binary("!=", receiver, Term.NULL));
    }
    var arguments = unbound ? new ArrayList<>(values.subList(1, values.size())) : values;
    var pre = contractPrecondition(functional, values, receiver, state, scope);
    var required = contractPrecondition(target, arguments, receiver, state, scope);
    obligation(state.assume(pre), required, reference, Diagnostic.Kind.REFINEMENT);
    var after = advance(receiver, state);
    var result =
        reference.getExecutable().isConstructor()
            ? receiver
            : fresh("referenceReturn", sort(functional.getType()));
    var guaranteed =
        contractPostcondition(target, arguments, receiver, result, after, state, scope);
    var promised = contractPostcondition(functional, values, receiver, result, after, state, scope);
    obligation(
        after.assume(pre).assume(guaranteed), promised, reference, Diagnostic.Kind.REFINEMENT);
  }

  private Term arrayContents(Term receiver, Term.Sort sort, TypingState state) {
    if (receiver instanceof Term.Choice choice)
      return new Term.Choice(
          choice.condition(),
          arrayContents(choice.yes(), sort, state),
          arrayContents(choice.no(), sort, state));
    if (possibleAliases.containsKey(receiver)) {
      var aliases = possibleAliases.get(receiver);
      Term contents = arrayContents(aliases.get(aliases.size() - 1), sort, state);
      for (int i = aliases.size() - 2; i >= 0; i--)
        contents =
            new Term.Choice(
                Term.equal(receiver, aliases.get(i)),
                arrayContents(aliases.get(i), sort, state),
                contents);
      return contents;
    }
    return arrayVersionContents(versionOf(receiver, state), sort, state);
  }

  private Term arrayVersionContents(Term version, Term.Sort sort, TypingState state) {
    if (version instanceof Term.Choice choice)
      return new Term.Choice(
          choice.condition(),
          arrayVersionContents(choice.yes(), sort, state),
          arrayVersionContents(choice.no(), sort, state));
    String key = "elements:" + version;
    return state.values().getOrDefault(key, new Term.ArraySymbol(key, sort));
  }

  private TypingState storeArray(Term receiver, Term contents, TypingState state) {
    if (receiver instanceof Term.Choice choice)
      return TypingState.join(
          choice.condition(),
          storeArray(choice.yes(), contents, state),
          storeArray(choice.no(), contents, state));
    if (possibleAliases.containsKey(receiver)) {
      var outcomes = new ArrayList<TypingState>();
      for (var alias : possibleAliases.get(receiver))
        outcomes.add(storeArray(alias, contents, state.assume(Term.equal(receiver, alias))));
      return mergeStates(outcomes);
    }
    var after = advance(receiver, state);
    return after.bind("elements:" + versionOf(receiver, after), contents);
  }

  private TypingState nonnull(Term value, TypingState state, CtElement site) {
    var present = Term.binary("!=", value, Term.NULL);
    possibleThrow("java.lang.NullPointerException", state.assume(Term.not(present)), site);
    return state.assume(present);
  }

  private TypingState arrayBounds(Term receiver, Term index, TypingState state, CtElement site) {
    var nonnull = Term.binary("!=", receiver, Term.NULL);
    var valid =
        Term.and(
            Term.binary(">=", index, Term.integer(0)),
            Term.binary("<", index, new Term.Call("length", List.of(receiver))));
    possibleThrow("java.lang.NullPointerException", state.assume(Term.not(nonnull)), site);
    possibleThrow(
        "java.lang.ArrayIndexOutOfBoundsException",
        state.assume(nonnull).assume(Term.not(valid)),
        site);
    return state.assume(nonnull).assume(valid);
  }

  private Location location(CtExpression<?> target, TypingState state, Scope scope) {
    if (target instanceof CtVariableAccess<?> variable) {
      var declaration =
          target instanceof CtFieldAccess<?> field
              ? ExternalSpecifications.resolve(field.getVariable())
              : variable.getVariable().getDeclaration();
      String key =
          declaration == null
              ? variable.getVariable().getSimpleName()
              : Annotations.key(declaration);
      Term owner = null;
      if (target instanceof CtFieldAccess<?> field) {
        var receiver = expression(field.getTarget(), state, scope);
        state = receiver.state();
        if (!field.getVariable().isStatic()) owner = receiver.value();
        if (!(field.getTarget() instanceof CtThisAccess<?>) && !field.getVariable().isStatic())
          key = fieldKey(field, receiver.value());
      }
      var value = state.values().getOrDefault(key, fresh("read", sort(target.getType())));
      if (owner != null && declaration instanceof CtField<?> field) {
        if (target.isParentInitialized()
            && (target.getParent() instanceof CtOperatorAssignment<?, ?>
                || target.getParent() instanceof CtUnaryOperator<?>))
          state = nonnull(owner, state, target);
        value = fieldValue(field, owner, state);
      }
      return new Location(state, value, key, declaration, owner, null);
    }
    if (target instanceof CtArrayWrite<?> array) {
      var receiver = expression(array.getTarget(), state, scope);
      var index = expression(array.getIndexExpression(), receiver.state(), scope);
      var compound =
          target.getParent() instanceof CtOperatorAssignment<?, ?>
              || target.getParent() instanceof CtUnaryOperator<?>;
      var checked =
          compound
              ? arrayBounds(receiver.value(), index.value(), index.state(), array)
              : index.state();
      return new Location(
          checked,
          new Term.ArraySelect(
              arrayContents(receiver.value(), sort(array.getType()), checked), index.value()),
          null,
          null,
          receiver.value(),
          index.value());
    }
    report(Diagnostic.Kind.UNSUPPORTED, "Unsupported assignment target", target);
    return new Location(state, fresh("target", sort(target.getType())), null, null, null, null);
  }

  private TypingState write(
      Location target, Term value, TypingState state, Scope scope, CtElement site) {
    if (target.declaration() != null
        && Annotations.has(target.declaration(), "Refinement")
        && eligible(target.declaration(), site))
      checkDeclared(target.declaration(), value, state, scope, site);
    if (target.index() != null) {
      state = arrayBounds(target.receiver(), target.index(), state, site);
      var contents =
          new Term.ArrayStore(
              arrayContents(target.receiver(), sort(((CtTypedElement<?>) site).getType()), state),
              target.index(),
              value);
      return storeArray(target.receiver(), contents, state);
    }
    if (target.receiver() != null && target.declaration() instanceof CtField<?> field) {
      state = nonnull(target.receiver(), state, site);
      var old = state;
      state = advance(target.receiver(), state);
      state = state.assume(frame(target.receiver(), field.getSimpleName() + "()", state, old));
      state = linkField(field, target.receiver(), value, state);
    }
    return target.key() == null ? state : state.bind(target.key(), value);
  }

  private Evaluation call(
      CtExecutableReference<?> reference,
      List<CtExpression<?>> arguments,
      CtExpression<?> target,
      CtExpression<?> call,
      TypingState before,
      Scope scope) {
    var receiver =
        target == null
            ? new Evaluation(before, before.values().getOrDefault("this", Term.NULL))
            : expression(target, before, scope);
    var state = receiver.state();
    var argumentValues = new ArrayList<Term>();
    for (var argument : arguments) {
      var evaluated = expression(argument, state, scope);
      state = evaluated.state();
      argumentValues.add(evaluated.value());
    }
    if (!reference.isStatic()
        && !reference.isConstructor()
        && target != null
        && !(target instanceof CtTypeAccess<?>)) state = nonnull(receiver.value(), state, call);
    var local = closureInvocation(call);
    if (local.isPresent() && !local.get().targets().isEmpty())
      return invokeClosure(
          local.get(), receiver.value(), argumentValues, arguments, call, state, scope);
    var declaration = Contracts.source(ExternalSpecifications.resolve(reference));
    boolean allocation = call instanceof CtConstructorCall<?>;
    boolean constructor = reference.isConstructor();
    Term result =
        allocation || sort(JavaTypes.result(call)) == Term.Sort.REF
            ? value(call, JavaTypes.result(call))
            : fresh("return", sort(JavaTypes.result(call)));
    state = state.assume(aliasMembership(result));
    if (allocation) {
      receiver = new Evaluation(state, result);
      state = allocateObject(call, result, JavaTypes.result(call), state);
    }
    if (reference.isConstructor()
        && reference.getDeclaringType() != null
        && reference.getDeclaringType().getQualifiedName().equals("java.lang.Object"))
      return new Evaluation(state, result);
    if (declaration == null) {
      // Unspecified calls can mutate any receiver/argument and fields reachable from this scope.
      state = havoc(state, scope);
      possibleThrow("java.lang.RuntimeException", state, call);
      for (var thrownType :
          reference.getExecutableDeclaration() == null
              ? Set.<CtTypeReference<? extends Throwable>>of()
              : reference.getExecutableDeclaration().getThrownTypes())
        possibleThrow(thrownType.getQualifiedName(), state, call);
      return new Evaluation(state.assume(bounds(result, JavaTypes.result(call))), result);
    }
    var choices = Contracts.sources(ExternalSpecifications.resolve(reference));
    if (choices.size() == 1) {
      var applied =
          applyContract(
              declaration,
              argumentValues,
              receiver,
              result,
              constructor,
              arguments,
              target,
              call,
              state,
              scope);
      return new Evaluation(constructionTransfers(call, applied.state()), applied.value());
    }
    Term accepted = Term.FALSE;
    for (var choice : choices)
      accepted =
          Term.or(
              accepted,
              contractPrecondition(choice, argumentValues, receiver.value(), state, scope));
    obligation(state, accepted, call, Diagnostic.Kind.REFINEMENT);
    var outcomes = new ArrayList<TypingState>();
    for (var choice : choices) {
      var pre = contractPrecondition(choice, argumentValues, receiver.value(), state, scope);
      outcomes.add(
          applyContract(
                  choice,
                  new ArrayList<>(argumentValues),
                  receiver,
                  result,
                  constructor,
                  arguments,
                  target,
                  call,
                  state.assume(pre),
                  scope)
              .state());
    }
    return new Evaluation(constructionTransfers(call, mergeStates(outcomes)), result);
  }

  private TypingState constructionTransfers(CtElement call, TypingState state) {
    var facts = ConstructionFacts.on(call);
    if (facts.isEmpty()) return state;
    return transferVersions(call, facts.get().transferredFrom(), state);
  }

  private TypingState transferVersions(
      CtElement call, Map<String, String> origins, TypingState state) {
    for (var entry : origins.entrySet()) {
      String source = entry.getValue();
      var visited = new HashSet<String>();
      while (origins.containsKey(source) && visited.add(source)) source = origins.get(source);
      var original = reference(call, source);
      var owned = reference(call, entry.getKey());
      state =
          state
              .version(receiverName(owned), versionOf(original, state))
              .assume(Term.equal(owned, original));
      if (referenceTypes.containsKey(original))
        referenceTypes.put(owned, referenceTypes.get(original));
    }
    return state;
  }

  private Evaluation applyContract(
      CtExecutable<?> declaration,
      List<Term> argumentValues,
      Evaluation receiver,
      Term result,
      boolean constructor,
      List<CtExpression<?>> arguments,
      CtExpression<?> target,
      CtExpression<?> call,
      TypingState state,
      Scope scope) {
    for (var thrownType :
        ExternalSpecifications.binding(declaration)
            .map(ExternalSpecifications.Binding::actual)
            .orElse(declaration)
            .getThrownTypes())
      possibleThrow(thrownType.getQualifiedName(), havoc(state, scope), call);
    if (mayThrow(declaration))
      possibleThrow("java.lang.RuntimeException", havoc(state, scope), call);
    for (int i = 0; i < Math.min(argumentValues.size(), declaration.getParameters().size()); i++)
      argumentValues.set(
          i, convert(argumentValues.get(i), declaration.getParameters().get(i).getType()));
    var bindings = new HashMap<String, Term>();
    bindings.put("this", receiver.value());
    for (int i = 0; i < Math.min(argumentValues.size(), declaration.getParameters().size()); i++)
      bindings.put(declaration.getParameters().get(i).getSimpleName(), argumentValues.get(i));
    for (int i = 0; i < Math.min(argumentValues.size(), declaration.getParameters().size()); i++) {
      var parameter = declaration.getParameters().get(i);
      var value = argumentValues.get(i);
      for (String text : Annotations.strings(parameter, "Refinement", "value")) {
        var actual = new HashMap<>(bindings);
        actual.put("_", value);
        if (eligibleArgument(arguments.get(i)))
          obligation(
              state,
              predicate(text, actual, state, state, scope, parameter),
              arguments.get(i),
              Diagnostic.Kind.REFINEMENT);
      }
    }
    var clauses = Annotations.named(declaration, "StateRefinement");
    if (constructor && clauses.isEmpty() && !ExternalSpecifications.isSpecification(declaration)) {
      String type = referenceTypes.get(receiver.value());
      var states =
          contracts.states().getOrDefault(type, List.of()).stream()
              .flatMap(Collection::stream)
              .collect(java.util.stream.Collectors.toSet());
      for (var ghost : contracts.ghosts())
        if (!contracts.fields().containsKey(ghost)
            && !states.contains(ghost)
            && contracts.belongsTo(ghost, type)
            && contracts.functions().get(ghost).parameters().size() == 1) {
          var sort = contracts.functions().get(ghost).result();
          Term initial =
              sort == Term.Sort.BOOL
                  ? Term.FALSE
                  : sort == Term.Sort.REF ? Term.NULL : new Term.Literal("0", sort);
          state =
              state.assume(
                  Term.binary(
                      "===",
                      new Term.Call(ghost, List.of(versionOf(receiver.value(), state))),
                      initial));
        }
    }
    if (!clauses.isEmpty()) {
      boolean safe =
          constructor || target == null || facts(target).map(LatteFacts::refinable).orElse(false);
      if (!safe)
        report(
            Diagnostic.Kind.SHARED_REFINEMENT,
            "A shared receiver cannot carry state refinements",
            call);
      var old = state;
      Term allowed = Term.FALSE;
      for (var annotation : clauses) {
        String from = String.join("", Annotations.strings(annotation.getValues().get("from")));
        allowed =
            Term.or(
                allowed,
                from.isBlank()
                    ? Term.TRUE
                    : predicate(from, bindings, old, old, scope, declaration));
      }
      boolean established = safe && obligation(old, allowed, call, Diagnostic.Kind.STATE);
      boolean changes =
          clauses.stream()
              .anyMatch(
                  a -> !String.join("", Annotations.strings(a.getValues().get("to"))).isBlank());
      if (changes) state = advance(receiver.value(), state);
      Term post = Term.FALSE;
      bindings.put(
          "_",
          constructor
                  || sort(JavaTypes.result(call)) == Term.Sort.REF
                      && "void".equals(String.valueOf(JavaTypes.result(call)))
              ? receiver.value()
              : result);
      bindings.put("return", result);
      for (var annotation : clauses) {
        String from = String.join("", Annotations.strings(annotation.getValues().get("from")));
        String to = String.join("", Annotations.strings(annotation.getValues().get("to")));
        var pre =
            from.isBlank() ? Term.TRUE : predicate(from, bindings, old, old, scope, declaration);
        var after =
            to.isBlank() ? Term.TRUE : predicate(to, bindings, state, old, scope, declaration);
        if (changes)
          after =
              Term.and(
                  after, frame(receiver.value(), to + modifiedFields(declaration), state, old));
        post = Term.or(post, Term.and(pre, after));
      }
      if (safe) {
        if (established && !prover.prove(old.assumptions(), Term.FALSE).proven()) {
          var consistency = prover.satisfiable(Term.and(state.assumptions(), post));
          if (!consistency.proven())
            report(
                Diagnostic.Kind.STATE,
                "State transition has no established consistent outcome: " + consistency.detail(),
                call);
        }
        state = state.assume(post);
      }
    } else if (!constructor
        && (MutationEffects.on(declaration).receiver()
            || MutationEffects.on(declaration).globals())) {
      state = advance(receiver.value(), state);
    }
    if (mayMutate(declaration)) {
      var effects = MutationEffects.on(declaration);
      var roots = new ArrayList<Map.Entry<CtExpression<?>, Term>>();
      for (int i = 0; i < arguments.size(); i++)
        if (effects.parameter(i) || effects.globals())
          roots.add(Map.entry(arguments.get(i), argumentValues.get(i)));
      if (target != null && (effects.receiver() || effects.globals()))
        roots.add(Map.entry(target, receiver.value()));
      state = havocReachable(call, roots, receiver.value(), state);
    }
    // An ordinary method can write fields even when its contract only describes its result.
    if (mayMutate(declaration)) {
      state = havocFields(state, scope);
    }
    state = registerObject(result, JavaTypes.result(call), state);
    bindings.put("_", result);
    for (String text : Annotations.strings(declaration, "Refinement", "value"))
      if (sort(JavaTypes.result(call)) != Term.Sort.REF
          || Annotations.permission(declaration).refinable())
        state = state.assume(predicate(text, bindings, state, state, scope, declaration));
    return new Evaluation(state.assume(bounds(result, JavaTypes.result(call))), result);
  }

  private TypingState havocReachable(
      CtExpression<?> call,
      List<Map.Entry<CtExpression<?>, Term>> roots,
      Term receiver,
      TypingState state) {
    var ownership = facts(call).map(LatteFacts::after);
    if (ownership.isEmpty()) return state;
    var affected = new HashMap<String, Term>();
    for (var root : roots) {
      var facts = facts(root.getKey());
      if (facts.isEmpty()) continue;
      for (String identity : facts.get().value().objects()) {
        var selected = Term.equal(root.getValue(), reference(call, identity));
        for (String reachable : ownership.get().reachable(Reference.object(identity)))
          affected.merge(reachable, selected, Term::or);
      }
    }
    // An unmodeled element set can include any previously read shared object.
    if (affected.keySet().stream().anyMatch(Reference::summarizesElements))
      // The callee's receiver postcondition already describes its final state, even when
      // an unknown argument element could alias it. Other potentially aliased objects lose facts.
      return havocObjects(state).version(receiverName(receiver), versionOf(receiver, state));
    for (var effect : affected.entrySet()) {
      var object = reference(call, effect.getKey());
      if (object.equals(receiver) || !state.versions().containsKey(receiverName(object))) continue;
      // The receiver's own transition was already applied, including conditional aliases.
      // Other objects are invalidated only on paths where an affected root reaches them.
      var changed = Term.and(effect.getValue(), Term.binary("!=", object, receiver));
      state =
          state.version(
              receiverName(object),
              new Term.Choice(changed, fresh("state", Term.Sort.REF), versionOf(object, state)));
      state = stateAxioms(object, state);
    }
    return state;
  }

  private Map<String, Term> contractBindings(
      CtExecutable<?> contract, List<Term> values, Term receiver) {
    var bindings = new HashMap<String, Term>();
    bindings.put("this", receiver);
    for (int i = 0; i < Math.min(values.size(), contract.getParameters().size()); i++)
      bindings.put(contract.getParameters().get(i).getSimpleName(), values.get(i));
    return bindings;
  }

  private Term contractPrecondition(
      CtExecutable<?> contract, List<Term> values, Term receiver, TypingState state, Scope scope) {
    var bindings = contractBindings(contract, values, receiver);
    Term pre = Term.TRUE;
    for (int i = 0; i < Math.min(values.size(), contract.getParameters().size()); i++) {
      bindings.put("_", values.get(i));
      for (String text :
          Annotations.strings(contract.getParameters().get(i), "Refinement", "value"))
        pre = Term.and(pre, predicate(text, bindings, state, state, scope, contract));
    }
    var clauses = Annotations.named(contract, "StateRefinement");
    if (!clauses.isEmpty()) {
      Term allowed = Term.FALSE;
      for (var clause : clauses) {
        String text = String.join("", Annotations.strings(clause.getValues().get("from")));
        allowed =
            Term.or(
                allowed,
                text.isBlank()
                    ? Term.TRUE
                    : predicate(text, bindings, state, state, scope, contract));
      }
      pre = Term.and(pre, allowed);
    }
    return pre;
  }

  private Term contractPostcondition(
      CtExecutable<?> contract,
      List<Term> values,
      Term receiver,
      Term result,
      TypingState state,
      TypingState old,
      Scope scope) {
    var bindings = contractBindings(contract, values, receiver);
    bindings.put("_", result);
    bindings.put("return", result);
    Term post = Term.TRUE;
    for (String text : Annotations.strings(contract, "Refinement", "value"))
      post = Term.and(post, predicate(text, bindings, state, old, scope, contract));
    var clauses = Annotations.named(contract, "StateRefinement");
    if (!clauses.isEmpty()) {
      Term alternatives = Term.FALSE;
      for (var clause : clauses) {
        String from = String.join("", Annotations.strings(clause.getValues().get("from")));
        String to = String.join("", Annotations.strings(clause.getValues().get("to")));
        Term pre =
            from.isBlank() ? Term.TRUE : predicate(from, bindings, old, old, scope, contract);
        Term after =
            to.isBlank() ? Term.TRUE : predicate(to, bindings, state, old, scope, contract);
        alternatives =
            Term.or(alternatives, Term.and(pre, Term.and(after, frame(receiver, to, state, old))));
      }
      post = Term.and(post, alternatives);
    }
    return post;
  }

  private void checkOverride(CtMethod<?> method, Scope scope) {
    var parents = Contracts.inherited(method);
    if (parents.isEmpty()) return;
    var state = TypingState.empty();
    var receiver = fresh("overrideReceiver", Term.Sort.REF);
    state =
        registerObject(receiver, method.getDeclaringType().getReference(), state)
            .bind("this", receiver);
    var arguments = new ArrayList<Term>();
    for (var parameter : method.getParameters()) {
      var value = fresh(parameter.getSimpleName(), sort(parameter.getType()));
      arguments.add(value);
      state =
          registerObject(value, parameter.getType(), state)
              .assume(bounds(value, parameter.getType()));
    }
    var after = advance(receiver, state);
    var result = fresh("overrideResult", sort(method.getType()));
    after = after.assume(bounds(result, method.getType()));
    var ownPre = contractPrecondition(method, arguments, receiver, state, scope);
    var ownPost = contractPostcondition(method, arguments, receiver, result, after, state, scope);
    for (var parent : parents) {
      for (int i = 0; i < method.getParameters().size(); i++) {
        var expected = Annotations.permission(parent.getParameters().get(i));
        var actual = Annotations.permission(method.getParameters().get(i));
        boolean compatible =
            expected == actual
                || expected == Permission.FREE
                || expected == Permission.UNIQUE && actual == Permission.BORROWED;
        if (!compatible)
          report(
              Diagnostic.Kind.PERMISSION,
              "Override changes the ownership requirement of parameter "
                  + method.getParameters().get(i).getSimpleName(),
              method);
      }
      var parentPre = contractPrecondition(parent, arguments, receiver, state, scope);
      if (!prover.prove(Term.and(state.assumptions(), parentPre), ownPre).proven())
        report(
            Diagnostic.Kind.REFINEMENT,
            "Override strengthens the precondition of " + parent.getSignature(),
            method);
      var parentPost =
          contractPostcondition(parent, arguments, receiver, result, after, state, scope);
      if (!prover
          .prove(Term.and(Term.and(after.assumptions(), parentPre), ownPost), parentPost)
          .proven())
        report(
            Diagnostic.Kind.REFINEMENT,
            "Override weakens the postcondition of " + parent.getSignature(),
            method);
    }
  }

  private void checkPreservedState(TypingState state, Scope scope, CtElement site) {
    var clauses = Annotations.named(scope.contract(), "StateRefinement");
    if (scope.entry() == null
        || clauses.isEmpty()
        || clauses.stream()
            .anyMatch(
                a ->
                    !Annotations.strings(a.getValues().get("to")).stream()
                        .allMatch(String::isBlank))) return;
    var receiver = state.values().get("this");
    if (!versionOf(receiver, state).equals(versionOf(receiver, scope.entry())))
      obligation(
          state,
          frame(
              receiver,
              String.join(
                  " ",
                  contracts.fields().values().stream().map(f -> f.getSimpleName() + "()").toList()),
              state,
              scope.entry()),
          site,
          Diagnostic.Kind.STATE);
  }

  private static Term defaultValue(CtTypeReference<?> type) {
    return switch (sort(type)) {
      case REF -> Term.NULL;
      case BOOL -> Term.FALSE;
      case INT -> Term.integer(0);
      case REAL -> Term.number("0.0");
      case FLOAT32, FLOAT64 -> new Term.Literal("0.0", sort(type));
    };
  }

  private List<CtField<?>> visibleFields(CtType<?> type) {
    var fields = new LinkedHashMap<String, CtField<?>>();
    for (var reference : type.getAllFields()) {
      var field = reference.getDeclaration();
      if (field != null && (!field.getDeclaringType().isShadow()))
        fields.putIfAbsent(field.getSimpleName(), field);
    }
    type.getFields().forEach(field -> fields.put(field.getSimpleName(), field));
    return List.copyOf(fields.values());
  }

  private void checkFields(TypingState state, Scope scope, CtElement site) {
    CtType<?> type = scope.executable().getParent(CtType.class);
    if (type == null) return;
    for (var field : type.getFields())
      if (!field.isStatic() && Annotations.has(field, "Refinement") && eligible(field, field))
        checkDeclared(field, state.values().get(Annotations.key(field)), state, scope, site);
  }

  private String modifiedFields(CtExecutable<?> executable) {
    var fields = MutationEffects.on(executable).roots().getOrDefault(-1, Set.of());
    if (fields.contains("*"))
      return contracts.fields().values().stream()
          .map(field -> " " + field.getSimpleName() + "()")
          .collect(java.util.stream.Collectors.joining());
    return fields.stream()
        .filter(name -> !name.startsWith("@"))
        .map(name -> " " + name.substring(name.lastIndexOf('#') + 1) + "()")
        .collect(java.util.stream.Collectors.joining());
  }

  private static boolean mayThrow(CtExecutable<?> executable) {
    if (executable.getBody() == null || ExternalSpecifications.isSpecification(executable))
      return true;
    return executable.getBody().getElements(new TypeFilter<>(CtElement.class)).stream()
        .anyMatch(
            e ->
                e instanceof CtThrow
                    || e instanceof CtInvocation<?>
                    || e instanceof CtConstructorCall<?>
                    || e instanceof CtFieldAccess<?>
                    || e instanceof CtArrayAccess<?, ?>
                    || e instanceof CtNewArray<?>
                    || e instanceof CtBinaryOperator<?> binary
                        && (binary.getKind() == BinaryOperatorKind.DIV
                            || binary.getKind() == BinaryOperatorKind.MOD));
  }

  private static boolean mayMutate(CtExecutable<?> executable) {
    return MutationEffects.on(executable).mutates();
  }

  private boolean eligibleArgument(CtExpression<?> expression) {
    var facts = facts(expression);
    if (facts.isPresent() && facts.get().refinable()) return true;
    report(
        Diagnostic.Kind.SHARED_REFINEMENT,
        "A shared argument cannot carry a refinement",
        expression);
    return false;
  }

  private boolean eligible(CtElement declaration, CtElement site) {
    if (declaration instanceof CtTypedElement<?> typed && sort(typed.getType()) != Term.Sort.REF)
      return true;
    var facts = facts(site);
    if (facts.isPresent()) {
      var fact = facts.get();
      var reference =
          declaration instanceof CtVariable<?>
              ? fact.after().variables().get(Annotations.key(declaration))
              : null;
      var permission = reference == null ? fact.permission() : fact.after().permission(reference);
      if (permission.refinable()) return true;
    } else if (facts(declaration).map(LatteFacts::refinable).orElse(false)) return true;
    report(
        Diagnostic.Kind.SHARED_REFINEMENT,
        "Refinement rejected: shared or unavailable object",
        site);
    return false;
  }

  private TypingState assumeDeclared(
      CtElement declaration, Term value, TypingState state, Scope scope) {
    for (String text : Annotations.strings(declaration, "Refinement", "value")) {
      var bindings = new HashMap<String, Term>();
      bindings.put("_", value);
      if (declaration instanceof CtNamedElement named) bindings.put(named.getSimpleName(), value);
      state = state.assume(predicate(text, bindings, state, state, scope, declaration));
    }
    return state;
  }

  private void checkDeclared(
      CtVariable<?> declaration, Term value, TypingState state, Scope scope, CtElement site) {
    var bindings = Map.of("_", value, declaration.getSimpleName(), value);
    for (String text : Annotations.strings(declaration, "Refinement", "value"))
      obligation(
          state,
          predicate(text, bindings, state, state, scope, site),
          site,
          Diagnostic.Kind.REFINEMENT);
  }

  private Term predicate(
      String source,
      Map<String, Term> explicit,
      TypingState state,
      TypingState old,
      Scope scope,
      CtElement site) {
    var parsed = PredicateParser.parse(source);
    var names = names(parsed);
    var bindings = new HashMap<String, Term>();
    scope
        .declarations()
        .forEach(
            (name, declaration) -> {
              if ((names.contains(name)
                      || names.contains("this." + name)
                      || names.stream().anyMatch(n -> n.startsWith(name + ".")))
                  && sort(declaration.getType()) == Term.Sort.REF) eligible(declaration, site);
              var value =
                  declaration instanceof CtField<?> field && !field.isStatic()
                      ? fieldValue(field, state.values().get("this"), state)
                      : state.values().get(Annotations.key(declaration));
              if (value != null) {
                bindings.put(name, value);
                bindings.put("this." + name, value);
              }
            });
    visibleLocals(site)
        .forEach(
            declaration -> {
              if ((names.contains(declaration.getSimpleName())
                      || names.stream()
                          .anyMatch(n -> n.startsWith(declaration.getSimpleName() + ".")))
                  && sort(declaration.getType()) == Term.Sort.REF) eligible(declaration, site);
              var value = state.values().get(Annotations.key(declaration));
              if (value != null) bindings.put(declaration.getSimpleName(), value);
            });
    bindings.put("this", state.values().getOrDefault("this", Term.NULL));
    bindings.putAll(explicit);
    if (explicit.containsKey("this") && !explicit.get("this").equals(state.values().get("this"))) {
      scope
          .declarations()
          .forEach(
              (name, declaration) -> {
                if (declaration instanceof CtField<?>) {
                  bindings.remove("this." + name);
                  if (!explicit.containsKey(name)) bindings.remove(name);
                }
              });
    }
    return predicates.resolve(parsed, bindings, state, old, site, 0);
  }

  private static Set<String> names(Term term) {
    if (term instanceof Term.Name name) return Set.of(name.value());
    List<Term> children =
        term instanceof Term.Unary unary
            ? List.of(unary.operand())
            : term instanceof Term.Binary binary
                ? List.of(binary.left(), binary.right())
                : term instanceof Term.Choice choice
                    ? List.of(choice.condition(), choice.yes(), choice.no())
                    : term instanceof Term.Call call ? call.arguments() : List.of();
    return children.stream()
        .flatMap(child -> names(child).stream())
        .collect(java.util.stream.Collectors.toSet());
  }

  private static List<CtLocalVariable<?>> visibleLocals(CtElement site) {
    var ancestors = new ArrayList<CtElement>();
    CtElement cursor = site;
    while (cursor != null && cursor.isParentInitialized()) {
      ancestors.add(cursor);
      cursor = cursor.getParent();
    }
    Collections.reverse(ancestors);
    var result = new ArrayList<CtLocalVariable<?>>();
    int offset =
        site.getPosition().isValidPosition()
            ? site.getPosition().getSourceStart()
            : Integer.MAX_VALUE;
    for (var ancestor : ancestors) {
      if (ancestor instanceof CtBlock<?> block)
        for (var statement : block.getStatements())
          if (statement instanceof CtLocalVariable<?> local
              && local.getPosition().isValidPosition()
              && local.getPosition().getSourceStart() <= offset) result.add(local);
      if (ancestor instanceof CtFor loop)
        for (var statement : loop.getForInit())
          if (statement instanceof CtLocalVariable<?> local) result.add(local);
      if (ancestor instanceof CtForEach loop) result.add(loop.getVariable());
    }
    return result;
  }

  private String referenceTypeOf(Term value) {
    if (value instanceof Term.Call call && call.name().equals("require:REF"))
      return referenceTypeOf(call.arguments().get(0));
    return referenceTypes.get(value);
  }

  private Term versionOf(Term value, TypingState state) {
    if (value instanceof Term.Call call && call.name().equals("require:REF"))
      return new Term.Call(call.name(), List.of(versionOf(call.arguments().get(0), state)));
    if (possibleAliases.containsKey(value)) {
      var aliases = possibleAliases.get(value);
      Term result = versionOf(aliases.get(aliases.size() - 1), state);
      for (int i = aliases.size() - 2; i >= 0; i--)
        result =
            new Term.Choice(
                Term.equal(value, aliases.get(i)), versionOf(aliases.get(i), state), result);
      return result;
    }
    if (value instanceof Term.Choice choice)
      return new Term.Choice(
          choice.condition(), versionOf(choice.yes(), state), versionOf(choice.no(), state));
    return state.versions().getOrDefault(receiverName(value), value);
  }

  private TypingState advance(Term value, TypingState state) {
    if (possibleAliases.containsKey(value)) {
      for (var candidate : possibleAliases.get(value)) {
        var next = fresh("state", Term.Sort.REF);
        state =
            state.version(
                receiverName(candidate),
                new Term.Choice(Term.equal(value, candidate), next, versionOf(candidate, state)));
        state = stateAxioms(candidate, state);
      }
      return state;
    }
    if (value instanceof Term.Choice choice) {
      var yes = advance(choice.yes(), state);
      var no = advance(choice.no(), state);
      return TypingState.join(choice.condition(), yes, no);
    }
    var version = fresh("state", Term.Sort.REF);
    var after = state.version(receiverName(value), version);
    return stateAxioms(value, after);
  }

  private Term frame(Term receiver, String postcondition, TypingState after, TypingState before) {
    String type = referenceTypes.get(receiver);
    if (type == null) return Term.TRUE;
    Set<String> modified = new HashSet<>();
    var pending = new ArrayDeque<String>();
    var matcher = java.util.regex.Pattern.compile("([\\w#$]+)\\s*\\(").matcher(postcondition);
    while (matcher.find()) pending.add(matcher.group(1));
    var visited = new HashSet<String>();
    while (!pending.isEmpty()) {
      var name = pending.removeFirst();
      if (!visited.add(name)) continue;
      for (String ghost : contracts.ghosts())
        if (ghost.equals(name) || ghost.endsWith("#" + name)) modified.add(ghost);
      contracts
          .aliases()
          .forEach(
              (alias, definition) -> {
                if (alias.equals(name) || alias.endsWith("#" + name))
                  pending.addAll(calledFunctions(definition.predicate()));
              });
    }
    for (var group : contracts.states().getOrDefault(type, List.of()))
      if (group.stream().anyMatch(n -> modified.contains(n))) modified.addAll(group);
    Term preserved = Term.TRUE;
    for (String ghost : contracts.ghosts())
      if (contracts.belongsTo(ghost, type)
          && !modified.contains(ghost)
          && contracts.functions().get(ghost).parameters().size() == 1)
        preserved =
            Term.and(
                preserved,
                Term.binary(
                    "===",
                    new Term.Call(ghost, List.of(versionOf(receiver, after))),
                    new Term.Call(ghost, List.of(versionOf(receiver, before)))));
    return preserved;
  }

  private static Set<String> calledFunctions(Term term) {
    var result = new HashSet<String>();
    if (term instanceof Term.Call call) {
      result.add(call.name());
      call.arguments().forEach(a -> result.addAll(calledFunctions(a)));
    } else if (term instanceof Term.Binary binary) {
      result.addAll(calledFunctions(binary.left()));
      result.addAll(calledFunctions(binary.right()));
    } else if (term instanceof Term.Unary unary) result.addAll(calledFunctions(unary.operand()));
    else if (term instanceof Term.Choice choice) {
      result.addAll(calledFunctions(choice.condition()));
      result.addAll(calledFunctions(choice.yes()));
      result.addAll(calledFunctions(choice.no()));
    }
    return Set.copyOf(result);
  }

  private Term fieldValue(CtField<?> field, Term receiver, TypingState state) {
    return new Term.Call(
        Contracts.qualify(field, field.getSimpleName()), List.of(versionOf(receiver, state)));
  }

  private TypingState linkField(CtField<?> field, Term receiver, Term value, TypingState state) {
    return value == null
        ? state
        : state.assume(Term.binary("===", fieldValue(field, receiver, state), value));
  }

  private TypingState registerObject(Term value, CtTypeReference<?> type, TypingState state) {
    if (sort(type) != Term.Sort.REF) return state;
    if (type != null) referenceTypes.put(value, type.getQualifiedName());
    if (!state.versions().containsKey(receiverName(value)))
      state = state.version(receiverName(value), fresh("state", Term.Sort.REF));
    return stateAxioms(value, state);
  }

  private TypingState allocateObject(
      CtElement site, Term value, CtTypeReference<?> type, TypingState state) {
    state = registerObject(value, type, state).assume(Term.binary("!=", value, Term.NULL));
    // Fresh Java allocations differ from objects that existed before this expression.
    // This lets a symbolic array index change one candidate without changing all the others.
    var facts = facts(site);
    if (facts.isPresent())
      for (String identity : facts.get().before().permissions().keySet()) {
        if (Reference.summarizesElements(identity)) continue;
        var previous = reference(site, identity);
        if (!previous.equals(value)) state = state.assume(Term.binary("!=", value, previous));
      }
    return state;
  }

  private TypingState stateAxioms(Term value, TypingState state) {
    String type = referenceTypes.get(value);
    if (type == null) return state;
    var version = versionOf(value, state);
    for (var group : contracts.states().getOrDefault(type, List.of())) {
      var predicates = group.stream().map(n -> (Term) new Term.Call(n, List.of(version))).toList();
      Term some = Term.FALSE;
      for (var predicate : predicates) some = Term.or(some, predicate);
      state = state.assume(some);
      for (int i = 0; i < predicates.size(); i++)
        for (int j = i + 1; j < predicates.size(); j++)
          state = state.assume(Term.not(Term.and(predicates.get(i), predicates.get(j))));
    }
    return state;
  }

  private TypingState havocObjects(TypingState state) {
    for (String key : state.versions().keySet())
      state = state.version(key, fresh("unknownState", Term.Sort.REF));
    return state;
  }

  private TypingState havocFields(TypingState state, Scope scope) {
    var retained = new HashMap<>(state.values());
    retained.keySet().removeIf(key -> key.startsWith("field:"));
    state = new TypingState(retained, state.versions(), state.assumptions());
    CtType<?> type = scope.executable().getParent(CtType.class);
    if (type != null)
      for (var field : visibleFields(type))
        if (!field.isFinal()) {
          var value =
              field.isStatic()
                  ? fresh(field.getSimpleName(), sort(field.getType()))
                  : fieldValue(field, state.values().get("this"), state);
          state = state.bind(Annotations.key(field), value).assume(bounds(value, field.getType()));
          if (!(scope.executable() instanceof CtConstructor<?>)
              && Annotations.has(field, "Refinement")
              && eligible(field, field)) state = assumeDeclared(field, value, state, scope);
        }
    return state;
  }

  private TypingState havoc(TypingState state, Scope scope) {
    return havocFields(havocObjects(state), scope);
  }

  private boolean obligation(
      TypingState state, Term expected, CtElement site, Diagnostic.Kind kind) {
    var proof = prover.prove(state.assumptions(), expected);
    if (!proof.proven())
      diagnostics.add(
          Diagnostic.at(
              kind,
              kind == Diagnostic.Kind.STATE
                  ? "Cannot establish state refinement"
                  : "Cannot establish refinement",
              "Obligation: " + expected + "\n" + proof.detail(),
              site));
    return proof.proven();
  }

  private Term value(CtElement site, CtTypeReference<?> type) {
    if (sort(type) != Term.Sort.REF)
      return fresh(site instanceof CtNamedElement n ? n.getSimpleName() : "value", sort(type));
    var reference =
        facts(site).map(LatteFacts::value).orElse(Reference.object(Annotations.key(site)));
    if (reference.objects().isEmpty() && reference.nullable()) return Term.NULL;
    if (reference.objects().size() == 1 && !reference.nullable())
      return referenceValue(site, reference.objects().iterator().next());
    var result = fresh("reference", Term.Sort.REF);
    if (!reference.objects().isEmpty()) {
      var candidates =
          new ArrayList<Term>(
              reference.objects().stream().sorted().map(id -> referenceValue(site, id)).toList());
      if (reference.nullable()) candidates.add(Term.NULL);
      possibleAliases.put(result, List.copyOf(candidates));
    }
    return result;
  }

  private Term referenceValue(CtElement site, String identity) {
    return Reference.summarizesElements(identity)
        ? fresh("arrayElement", Term.Sort.REF)
        : reference(site, identity);
  }

  private Term aliasMembership(Term value) {
    if (!possibleAliases.containsKey(value)) return Term.TRUE;
    return possibleAliases.get(value).stream()
        .map(candidate -> Term.equal(value, candidate))
        .reduce(Term.FALSE, Term::or);
  }

  private Term reference(CtElement site, String key) {
    return new Term.Symbol("object:" + key, Term.Sort.REF);
  }

  private Term.Symbol fresh(String name, Term.Sort sort) {
    return new Term.Symbol(name + "$" + counter++, sort);
  }

  private static String receiverName(Term value) {
    return value.toString();
  }

  private static String fieldKey(CtFieldAccess<?> field, Term receiver) {
    return "field:" + receiver + ":" + field.getVariable().getQualifiedName();
  }

  private static Term.Sort sort(CtTypeReference<?> type) {
    return type == null ? Term.Sort.REF : Contracts.sort(type.getQualifiedName());
  }

  private Term literal(CtLiteral<?> literal) {
    Object value = literal.getValue();
    if (value == null) return Term.NULL;
    if (value instanceof Boolean b) return b ? Term.TRUE : Term.FALSE;
    if (value instanceof Character c) return Term.integer(c);
    if (value instanceof Float) return new Term.Literal(value.toString(), Term.Sort.FLOAT32);
    if (value instanceof Double) return new Term.Literal(value.toString(), Term.Sort.FLOAT64);
    if (value instanceof Number number) return Term.number(number.toString());
    return new Term.Literal(value.toString(), Term.Sort.REF);
  }

  private void report(Diagnostic.Kind kind, String message, CtElement site) {
    if (kind == Diagnostic.Kind.SYNTAX
        && message != null
        && message.startsWith("Argument mismatch:")) kind = Diagnostic.Kind.ARGUMENT;
    diagnostics.add(Diagnostic.at(kind, message == null ? kind.name() : message, site));
  }
}
