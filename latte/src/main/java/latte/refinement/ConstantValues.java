package latte.refinement;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

/**
 * Resolves compile-time constants from source and selected JDK APIs without executing user code.
 */
final class ConstantValues {
  private ConstantValues() {}

  static Optional<Term> resolve(String name, CtElement site) {
    var constant = builtin(name);
    if (constant != null) return Optional.of(constant);
    if (!name.contains(".")) {
      CtType<?> owner = site instanceof CtType<?> type ? type : site.getParent(CtType.class);
      if (owner != null) {
        var field = owner.getField(name);
        if (field != null && field.isStatic() && field.isFinal())
          return expression(field.getDefaultExpression(), new HashSet<>());
      }
      if (site.getPosition().isValidPosition())
        for (var imported : site.getPosition().getCompilationUnit().getImports()) {
          if (imported.getReference() instanceof spoon.reflect.reference.CtFieldReference<?> field
              && field.getSimpleName().equals(name))
            return resolve(field.getQualifiedName().replace('#', '.'), site);
          if (imported.getReference()
              instanceof spoon.reflect.reference.CtTypeMemberWildcardImportReference wildcard) {
            var resolved =
                resolve(wildcard.getTypeReference().getQualifiedName() + "." + name, site);
            if (resolved.isPresent()) return resolved;
          }
        }
      return Optional.empty();
    }
    int dot = name.lastIndexOf('.');
    String typeName = name.substring(0, dot), fieldName = name.substring(dot + 1);
    CtType<?> enclosing = site instanceof CtType<?> type ? type : site.getParent(CtType.class);
    String packageName =
        enclosing == null || enclosing.getPackage() == null
            ? ""
            : enclosing.getPackage().getQualifiedName();
    var allowed = new HashSet<String>();
    allowed.add(typeName);
    if (!typeName.contains(".")) {
      allowed.add(packageName.isEmpty() ? typeName : packageName + "." + typeName);
      for (CtType<?> owner = enclosing; owner != null; owner = owner.getDeclaringType())
        allowed.add(owner.getQualifiedName() + "$" + typeName);
      if (site.getPosition().isValidPosition())
        for (var imported : site.getPosition().getCompilationUnit().getImports())
          if (imported.getReference() instanceof CtTypeReference<?> reference
              && reference.getSimpleName().equals(typeName))
            allowed.add(reference.getQualifiedName());
    }
    if (allowed.contains("javax.imageio.ImageWriteParam")) {
      var value =
          switch (fieldName) {
            case "MODE_DISABLED" -> 0;
            case "MODE_DEFAULT" -> 1;
            case "MODE_EXPLICIT" -> 2;
            case "MODE_COPY_FROM_METADATA" -> 3;
            default -> null;
          };
      if (value != null) return Optional.of(Term.integer(value));
    }
    if (allowed.contains("java.util.zip.ZipFile")) {
      var value =
          switch (fieldName) {
            case "OPEN_READ" -> 1;
            case "OPEN_DELETE" -> 4;
            default -> null;
          };
      if (value != null) return Optional.of(Term.integer(value));
    }
    for (var type :
        site.getFactory()
            .getModel()
            .getElements(new spoon.reflect.visitor.filter.TypeFilter<>(CtType.class))) {
      if (!allowed.contains(type.getQualifiedName())) continue;
      var field = type.getField(fieldName);
      if (field instanceof CtEnumValue<?>)
        return Optional.of(
            new Term.Symbol("enum:" + type.getQualifiedName() + "." + fieldName, Term.Sort.REF));
      if (field != null && field.isStatic() && field.isFinal())
        return expression(field.getDefaultExpression(), new HashSet<>());
      if (type instanceof CtEnum<?> enumeration
          && enumeration.getEnumValues().stream()
              .anyMatch(e -> e.getSimpleName().equals(fieldName)))
        return Optional.of(
            new Term.Symbol("enum:" + type.getQualifiedName() + "." + fieldName, Term.Sort.REF));
    }
    return Optional.empty();
  }

  private static Optional<Term> expression(CtExpression<?> expression, Set<CtField<?>> visited) {
    return expressionValue(expression, visited)
        .map(value -> JavaArithmetic.cast(value, expression));
  }

  private static Optional<Term> expressionValue(
      CtExpression<?> expression, Set<CtField<?>> visited) {
    if (expression instanceof CtLiteral<?> literal) {
      Object value = literal.getValue();
      if (value instanceof Boolean b) return Optional.of(b ? Term.TRUE : Term.FALSE);
      if (value instanceof Float)
        return Optional.of(new Term.Literal(value.toString(), Term.Sort.FLOAT32));
      if (value instanceof Double)
        return Optional.of(new Term.Literal(value.toString(), Term.Sort.FLOAT64));
      if (value instanceof String)
        return Optional.of(new Term.Literal(value.toString(), Term.Sort.REF));
      if (value instanceof Character c) return Optional.of(Term.integer(c));
      if (value instanceof Byte
          || value instanceof Short
          || value instanceof Integer
          || value instanceof Long) return Optional.of(Term.number(value.toString()));
    }
    if (expression instanceof CtUnaryOperator<?> unary && unary.getKind() == UnaryOperatorKind.NEG)
      return expression(unary.getOperand(), visited)
          .map(value -> JavaArithmetic.wrap(new Term.Unary("-", value), unary.getType()));
    if (expression instanceof CtFieldRead<?> read) {
      var builtin = builtin(read.getVariable().getQualifiedName().replace('#', '.'));
      if (builtin != null) return Optional.of(builtin);
      var field = read.getVariable().getDeclaration();
      if (field != null && field.isStatic() && field.isFinal() && visited.add(field))
        return expression(field.getDefaultExpression(), visited);
    }
    if (expression instanceof CtBinaryOperator<?> binary) {
      var left = expression(binary.getLeftHandOperand(), new HashSet<>(visited));
      var right = expression(binary.getRightHandOperand(), new HashSet<>(visited));
      if (left.isPresent() && right.isPresent())
        return Optional.of(
            JavaArithmetic.cast(
                JavaArithmetic.binary(binary.getKind(), left.get(), right.get(), binary.getType()),
                binary));
    }
    if (expression instanceof CtConditional<?> conditional) {
      var guard = expression(conditional.getCondition(), new HashSet<>(visited));
      var yes = expression(conditional.getThenExpression(), new HashSet<>(visited));
      var no = expression(conditional.getElseExpression(), new HashSet<>(visited));
      if (guard.isPresent() && yes.isPresent() && no.isPresent())
        return Optional.of(new Term.Choice(guard.get(), yes.get(), no.get()));
    }
    return Optional.empty();
  }

  private static Term builtin(String name) {
    return switch (name) {
      case "Math.PI", "java.lang.Math.PI" ->
          new Term.Literal(Double.toString(Math.PI), Term.Sort.FLOAT64);
      case "Math.E", "java.lang.Math.E" ->
          new Term.Literal(Double.toString(Math.E), Term.Sort.FLOAT64);
      case "Float.MAX_VALUE", "java.lang.Float.MAX_VALUE" ->
          new Term.Literal(Float.toString(Float.MAX_VALUE), Term.Sort.FLOAT32);
      case "Float.MIN_VALUE", "java.lang.Float.MIN_VALUE" ->
          new Term.Literal(Float.toString(Float.MIN_VALUE), Term.Sort.FLOAT32);
      case "Float.MIN_NORMAL", "java.lang.Float.MIN_NORMAL" ->
          new Term.Literal(Float.toString(Float.MIN_NORMAL), Term.Sort.FLOAT32);
      case "Double.MAX_VALUE", "java.lang.Double.MAX_VALUE" ->
          new Term.Literal(Double.toString(Double.MAX_VALUE), Term.Sort.FLOAT64);
      case "Double.MIN_VALUE", "java.lang.Double.MIN_VALUE" ->
          new Term.Literal(Double.toString(Double.MIN_VALUE), Term.Sort.FLOAT64);
      case "Double.MIN_NORMAL", "java.lang.Double.MIN_NORMAL" ->
          new Term.Literal(Double.toString(Double.MIN_NORMAL), Term.Sort.FLOAT64);
      case "Double.NaN", "java.lang.Double.NaN" -> new Term.Literal("NaN", Term.Sort.FLOAT64);
      case "Double.POSITIVE_INFINITY", "java.lang.Double.POSITIVE_INFINITY" ->
          new Term.Literal("Infinity", Term.Sort.FLOAT64);
      case "Double.NEGATIVE_INFINITY", "java.lang.Double.NEGATIVE_INFINITY" ->
          new Term.Literal("-Infinity", Term.Sort.FLOAT64);
      case "Float.NaN", "java.lang.Float.NaN" -> new Term.Literal("NaN", Term.Sort.FLOAT32);
      case "Float.POSITIVE_INFINITY", "java.lang.Float.POSITIVE_INFINITY" ->
          new Term.Literal("Infinity", Term.Sort.FLOAT32);
      case "Float.NEGATIVE_INFINITY", "java.lang.Float.NEGATIVE_INFINITY" ->
          new Term.Literal("-Infinity", Term.Sort.FLOAT32);
      case "Byte.MIN_VALUE", "java.lang.Byte.MIN_VALUE" -> Term.integer(Byte.MIN_VALUE);
      case "Byte.MAX_VALUE", "java.lang.Byte.MAX_VALUE" -> Term.integer(Byte.MAX_VALUE);
      case "Short.MIN_VALUE", "java.lang.Short.MIN_VALUE" -> Term.integer(Short.MIN_VALUE);
      case "Short.MAX_VALUE", "java.lang.Short.MAX_VALUE" -> Term.integer(Short.MAX_VALUE);
      case "Integer.MIN_VALUE", "java.lang.Integer.MIN_VALUE" -> Term.integer(Integer.MIN_VALUE);
      case "Integer.MAX_VALUE", "java.lang.Integer.MAX_VALUE" -> Term.integer(Integer.MAX_VALUE);
      case "Long.MIN_VALUE", "java.lang.Long.MIN_VALUE" -> Term.integer(Long.MIN_VALUE);
      case "Long.MAX_VALUE", "java.lang.Long.MAX_VALUE" -> Term.integer(Long.MAX_VALUE);
      case "Character.MIN_VALUE", "java.lang.Character.MIN_VALUE" ->
          Term.integer(Character.MIN_VALUE);
      case "Character.MAX_VALUE", "java.lang.Character.MAX_VALUE" ->
          Term.integer(Character.MAX_VALUE);
      default -> null;
    };
  }
}
