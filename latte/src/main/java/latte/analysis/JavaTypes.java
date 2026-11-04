package latte.analysis;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

/** Contextual types for no-classpath expressions, using Java's required operand conversions. */
public final class JavaTypes {
  private JavaTypes() {}

  public static Optional<String> unboxed(CtTypeReference<?> type) {
    if (type == null) return Optional.empty();
    return Optional.ofNullable(
        switch (type.getQualifiedName()) {
          case "java.lang.Boolean" -> "boolean";
          case "java.lang.Byte" -> "byte";
          case "java.lang.Short" -> "short";
          case "java.lang.Character" -> "char";
          case "java.lang.Integer" -> "int";
          case "java.lang.Long" -> "long";
          case "java.lang.Float" -> "float";
          case "java.lang.Double" -> "double";
          default -> null;
        });
  }

  public static CtTypeReference<?> result(CtExpression<?> expression) {
    if (expression.getType() != null) return expression.getType();
    String inferred = expected(expression);
    return inferred == null ? null : expression.getFactory().Type().createReference(inferred);
  }

  public static String expected(CtExpression<?> expression) {
    if (!expression.isParentInitialized()) return null;
    var parent = expression.getParent();
    if (parent instanceof CtBinaryOperator<?> binary) {
      var other =
          binary.getLeftHandOperand() == expression
              ? binary.getRightHandOperand()
              : binary.getLeftHandOperand();
      var otherType = other.getType();
      return switch (binary.getKind()) {
        case AND, OR -> "boolean";
        case EQ, NE -> otherType == null ? null : otherType.getQualifiedName();
        case INSTANCEOF -> null;
        default ->
            otherType != null ? unboxed(otherType).orElse(otherType.getQualifiedName()) : "int";
      };
    }
    if (parent instanceof CtUnaryOperator<?> unary)
      return unary.getKind() == UnaryOperatorKind.NOT ? "boolean" : "int";
    if (parent instanceof CtVariable<?> variable && variable.getType() != null)
      return variable.getType().getQualifiedName();
    if (parent instanceof CtAssignment<?, ?> assignment
        && assignment.getAssigned().getType() != null)
      return assignment.getAssigned().getType().getQualifiedName();
    if (parent instanceof CtReturn<?> returned) {
      var method = returned.getParent(CtExecutable.class);
      if (method instanceof CtMethod<?> m) return m.getType().getQualifiedName();
    }
    if (parent instanceof CtIf || parent instanceof CtWhile || parent instanceof CtDo)
      return "boolean";
    if (parent instanceof CtConditional<?> conditional)
      return conditional.getCondition() == expression
          ? "boolean"
          : conditional.getType() == null ? null : conditional.getType().getQualifiedName();
    return null;
  }
}
