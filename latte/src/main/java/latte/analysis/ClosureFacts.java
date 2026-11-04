package latte.analysis;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;

/** Captured bindings are immutable heap edges; the referenced objects remain mutable. */
public record ClosureFacts(Set<String> bindings, boolean deferred, boolean escaped) {
  public static final String KEY = "latte.analysis.closure.v1";
  public static final String CAPTURE = "capture:";
  public static final String RECEIVER = "@receiver";

  public ClosureFacts {
    bindings = Set.copyOf(bindings);
  }

  public static Optional<ClosureFacts> on(CtElement node) {
    return Optional.ofNullable(node.getMetadata(KEY))
        .filter(ClosureFacts.class::isInstance)
        .map(ClosureFacts.class::cast);
  }

  public void attach(CtElement node) {
    node.putMetadata(KEY, this);
  }

  public static CtMethod<?> functional(CtExpression<?> expression) {
    if (expression instanceof CtLambda<?> lambda) return lambda.getOverriddenMethod();
    var type = expression.getType() == null ? null : expression.getType().getTypeDeclaration();
    return type == null
        ? null
        : type.getAllMethods().stream()
            .filter(
                m ->
                    m.isAbstract()
                        && !m.isStatic()
                        && !m.getDeclaringType().getQualifiedName().equals("java.lang.Object"))
            .findFirst()
            .orElse(null);
  }
}
