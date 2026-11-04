package latte.analysis;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Conservative future uses, including back edges, handlers, cleanup and deferred captures. */
final class LocalObservability {
  private LocalObservability() {}

  /**
   * Inference is restricted to completed statements. An enclosing expression can retain a receiver
   * or an earlier operand in a Java evaluation temporary that is not a local-variable binding.
   */
  static Optional<CtStatement> boundary(CtElement site) {
    if (site instanceof CtReturn<?> returned) return Optional.of(returned);
    var parent = site.getParent();
    if (parent instanceof CtLocalVariable<?> local && local.getDefaultExpression() == site)
      return Optional.of(local);
    if (parent instanceof CtReturn<?> returned && returned.getReturnedExpression() == site)
      return Optional.of(returned);
    if (parent instanceof CtAssignment<?, ?> assignment
        && !(assignment instanceof CtOperatorAssignment<?, ?>)
        && assignment.getAssignment() == site
        && assignment.getAssigned() instanceof CtVariableWrite<?>
        && !(assignment.getAssigned() instanceof CtFieldWrite<?>)) return Optional.of(assignment);
    if (site instanceof CtStatement statement
        && (parent instanceof CtBlock<?>
            || parent instanceof CtCase<?>
            || parent instanceof CtIf branch
                && (branch.getThenStatement() == site || branch.getElseStatement() == site)
            || parent instanceof CtLoop loop && loop.getBody() == site))
      return Optional.of(statement);
    return Optional.empty();
  }

  static Set<String> after(
      CtStatement boundary, CtExecutable<?> executable, Set<String> annotationNames) {
    var uses = new HashSet<String>();
    CtElement child = boundary;
    while (child != executable && child.isParentInitialized()) {
      var parent = child.getParent();
      if (parent instanceof CtBlock<?> block) following(block.getStatements(), child, uses);
      else if (parent instanceof CtLoop) reads(parent, uses); // The next iteration may use it.
      else if (parent instanceof CtTry tried) {
        tried.getCatchers().forEach(catcher -> reads(catcher, uses));
        reads(tried.getFinalizer(), uses);
        if (tried instanceof CtTryWithResource resources)
          for (var resource : resources.getResources()) {
            reads(resource, uses);
            if (resource instanceof CtVariable<?> variable) uses.add(Annotations.key(variable));
          }
      } else if (parent instanceof CtCase<?> branch) following(branch.getStatements(), child, uses);
      else if (parent instanceof CtAbstractSwitch<?>) reads(parent, uses); // Fall-through.
      else if (parent instanceof CtSynchronized lock) reads(lock.getExpression(), uses);
      child = parent;
    }
    // Local/anonymous classes do not yet have the explicit capture edges used for lambdas. Keep
    // their lexical captures observable even if their allocation occurred before this statement.
    if (executable.getBody() != null)
      executable
          .getBody()
          .getElements(new TypeFilter<>(CtType.class))
          .forEach(type -> reads(type, uses));
    // Refinement predicates can observe a binding without a Java variable-read node. Keeping all
    // annotation-mentioned bindings for this executable is deliberately more conservative than
    // trying to interpret refinement syntax in the ownership pass.
    for (var variable : executable.getElements(new TypeFilter<>(CtVariable.class)))
      if (annotationNames.contains(variable.getSimpleName())) uses.add(Annotations.key(variable));
    return Set.copyOf(uses);
  }

  static Set<String> annotationNames(spoon.reflect.CtModel model) {
    var names = new HashSet<String>();
    var identifiers =
        java.util.regex.Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    // Include alias/predicate declarations outside the executable: a local annotation may refer
    // to one of these without spelling the name of the observed binding itself.
    for (var annotation : model.getElements(new TypeFilter<>(CtAnnotation.class)))
      for (Object argument : annotation.getValues().values())
        if (argument instanceof CtExpression<?> expression)
          for (String text : Annotations.strings(expression))
            identifiers.matcher(text).results().forEach(match -> names.add(match.group()));
    return Set.copyOf(names);
  }

  private static void following(List<CtStatement> statements, CtElement child, Set<String> uses) {
    boolean found = false;
    for (var statement : statements) {
      if (found) reads(statement, uses);
      if (statement == child) found = true;
    }
  }

  private static void reads(CtElement element, Set<String> uses) {
    if (element == null) return;
    for (var read : element.getElements(new TypeFilter<>(CtVariableRead.class))) {
      var declaration = read.getVariable().getDeclaration();
      if (declaration != null) uses.add(Annotations.key(declaration));
    }
  }
}
