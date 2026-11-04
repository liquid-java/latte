package latte.analysis;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;

/** Reads Spoon annotation syntax without loading user classes or executing annotation proxies. */
public final class Annotations {
  private Annotations() {}

  public static boolean has(CtElement node, String name) {
    return !named(node, name).isEmpty();
  }

  public static List<CtAnnotation<?>> named(CtElement node, String name) {
    if (node == null) return List.of();
    return node.getAnnotations().stream()
        .filter(a -> a.getAnnotationType().getSimpleName().equals(name))
        .<CtAnnotation<?>>map(a -> a)
        .toList();
  }

  public static List<String> strings(CtElement node, String annotation, String key) {
    return named(node, annotation).stream()
        .flatMap(a -> strings(a.getValues().get(key)).stream())
        .toList();
  }

  public static List<String> strings(CtExpression<?> value) {
    if (value instanceof CtLiteral<?> literal) return List.of(String.valueOf(literal.getValue()));
    if (value instanceof CtNewArray<?> array)
      return array.getElements().stream().flatMap(e -> strings(e).stream()).toList();
    if (value instanceof CtBinaryOperator<?> binary && binary.getKind() == BinaryOperatorKind.PLUS)
      return List.of(
          String.join("", strings(binary.getLeftHandOperand()))
              + String.join("", strings(binary.getRightHandOperand())));
    return List.of();
  }

  public static Permission permission(CtElement node) {
    if (node instanceof CtTypedElement<?> typed
        && typed.getType() != null
        && typed.getType().isPrimitive()) return Permission.IMMUTABLE;
    for (var permission :
        List.of(Permission.SHARED, Permission.FREE, Permission.BORROWED, Permission.UNIQUE)) {
      String name =
          permission.name().charAt(0) + permission.name().substring(1).toLowerCase(Locale.ROOT);
      if (has(node, name)) return permission;
    }
    return Permission.SHARED;
  }

  public static String key(CtElement node) {
    var position = node.getPosition();
    String owner =
        node instanceof CtNamedElement named
            ? named.getSimpleName()
            : node.getClass().getSimpleName();
    return position.isValidPosition()
        ? position.getFile() + ":" + position.getSourceStart() + ":" + owner
        : owner;
  }
}
