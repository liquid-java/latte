package latte.analysis;

import java.util.*;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Finite, compositional return paths. Recursive paths widen to a reachable-subgraph suffix. */
final class ReturnAliases {
  static final int RECEIVER = -1, FRESH = -2, EXTERNAL = -3, NULL = -4;

  record Path(int root, List<String> fields) {
    Path {
      fields = List.copyOf(fields);
    }

    Path append(List<String> suffix) {
      if (fields.contains("*")) return this;
      var path = new ArrayList<>(fields);
      path.addAll(suffix);
      if (path.size() > 4)
        path = new ArrayList<>(List.of(path.get(0), path.get(1), path.get(2), "*"));
      return new Path(root, path);
    }
  }

  private final Map<CtExecutable<?>, Set<Path>> summaries;

  private ReturnAliases(Map<CtExecutable<?>, Set<Path>> summaries) {
    this.summaries = Map.copyOf(summaries);
  }

  static ReturnAliases collect(CtModel model) {
    var methods =
        model.getElements(new TypeFilter<>(CtExecutable.class)).stream()
            .filter(e -> e instanceof CtMethod<?> || e instanceof CtConstructor<?>)
            .filter(e -> !ExternalSpecifications.isLibrary(e))
            .toList();
    Map<CtExecutable<?>, Set<Path>> summaries = Map.of();
    for (; ; ) {
      var next = new HashMap<>(summaries);
      for (CtExecutable<?> method : methods) {
        var paths = new HashSet<>(summaries.getOrDefault(method, Set.of()));
        if (ExternalSpecifications.isSpecification(method)) {
          next.put(method, Set.of());
          continue;
        }
        for (var returned : method.getElements(new TypeFilter<>(CtReturn.class)))
          if (returned.getParent(CtExecutable.class) == method)
            paths.addAll(
                expression(returned.getReturnedExpression(), method, summaries, new HashSet<>()));
        next.put(method, Set.copyOf(paths));
      }
      if (next.equals(summaries)) return new ReturnAliases(next);
      summaries = Map.copyOf(next);
    }
  }

  Set<Path> paths(CtExecutable<?> executable) {
    return executable == null ? Set.of() : summaries.getOrDefault(executable, Set.of());
  }

  Set<Path> origins(CtExpression<?> expression, CtExecutable<?> owner) {
    return expression(expression, owner, summaries, new HashSet<>());
  }

  private static Set<Path> expression(
      CtExpression<?> expression,
      CtExecutable<?> owner,
      Map<CtExecutable<?>, Set<Path>> summaries,
      Set<CtVariable<?>> visiting) {
    if (expression == null) return Set.of();
    if (expression instanceof CtLiteral<?> literal)
      return literal.getValue() == null ? Set.of(new Path(NULL, List.of())) : Set.of();
    if (expression instanceof CtThisAccess<?> || expression instanceof CtSuperAccess<?>)
      return Set.of(new Path(RECEIVER, List.of()));
    if (expression instanceof CtConstructorCall<?> || expression instanceof CtNewArray<?>)
      return Set.of(new Path(FRESH, List.of()));
    if (expression instanceof CtFieldAccess<?> field) {
      if (field.getVariable().isStatic()) return Set.of(new Path(EXTERNAL, List.of()));
      return append(
          expression(field.getTarget(), owner, summaries, visiting),
          List.of(field.getVariable().getQualifiedName()));
    }
    if (expression instanceof CtArrayAccess<?, ?> array)
      return append(
          expression(array.getTarget(), owner, summaries, visiting),
          List.of(ArraySlots.at(array.getIndexExpression())));
    if (expression instanceof CtVariableAccess<?> access) {
      var declaration = access.getVariable().getDeclaration();
      int index = declaration == null ? -1 : owner.getParameters().indexOf(declaration);
      if (index >= 0) return Set.of(new Path(index, List.of()));
      if (declaration == null || !visiting.add(declaration)) return Set.of();
      var paths =
          new HashSet<>(expression(declaration.getDefaultExpression(), owner, summaries, visiting));
      for (var assignment : owner.getElements(new TypeFilter<>(CtAssignment.class)))
        if (assignment.getAssigned() instanceof CtVariableAccess<?> variable
            && variable.getVariable().getDeclaration() == declaration)
          paths.addAll(expression(assignment.getAssignment(), owner, summaries, visiting));
      visiting.remove(declaration);
      return Set.copyOf(paths);
    }
    if (expression instanceof CtConditional<?> conditional) {
      var paths =
          new HashSet<>(expression(conditional.getThenExpression(), owner, summaries, visiting));
      paths.addAll(expression(conditional.getElseExpression(), owner, summaries, visiting));
      return Set.copyOf(paths);
    }
    if (expression instanceof CtInvocation<?> call) {
      var declaration = ExternalSpecifications.resolve(call.getExecutable());
      if (declaration == null) return Set.of(new Path(EXTERNAL, List.of()));
      if (Annotations.permission(declaration) == Permission.FREE)
        return Set.of(new Path(FRESH, List.of()));
      var paths = new HashSet<Path>();
      for (var path : summaries.getOrDefault(declaration, Set.of())) {
        if (path.root() < RECEIVER) paths.add(path);
        else {
          var actual =
              path.root() == RECEIVER
                  ? call.getTarget()
                  : path.root() < call.getArguments().size()
                      ? call.getArguments().get(path.root())
                      : null;
          paths.addAll(append(expression(actual, owner, summaries, visiting), path.fields()));
        }
      }
      return Set.copyOf(paths);
    }
    return Set.of(new Path(EXTERNAL, List.of()));
  }

  private static Set<Path> append(Set<Path> paths, List<String> fields) {
    return paths.stream()
        .map(p -> p.append(fields))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
