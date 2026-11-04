package latte.analysis;

import java.util.*;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;

/** Call-specific ownership facts for locally known closures, published on the invocation AST. */
public record ClosureInvocation(List<Target> targets) {
  public static final String KEY = "latte.analysis.closure.call.v1";

  public record Target(
      String identity,
      CtExpression<?> expression,
      Map<String, Reference> captures,
      Map<CtElement, LatteFacts> facts,
      Map<CtElement, ClosureInvocation> invocations,
      Set<String> mutations,
      Map<String, String> inputTransfers,
      Map<String, String> outputTransfers) {
    public Target {
      captures = Map.copyOf(captures);
      facts = Collections.unmodifiableMap(new IdentityHashMap<>(facts));
      invocations = Collections.unmodifiableMap(new IdentityHashMap<>(invocations));
      mutations = Set.copyOf(mutations);
      inputTransfers = Map.copyOf(inputTransfers);
      outputTransfers = Map.copyOf(outputTransfers);
    }
  }

  public ClosureInvocation {
    targets = List.copyOf(targets);
  }

  public void attach(CtElement node) {
    node.putMetadata(KEY, this);
  }

  public static Optional<ClosureInvocation> on(CtElement node) {
    return Optional.ofNullable(node.getMetadata(KEY))
        .filter(ClosureInvocation.class::isInstance)
        .map(ClosureInvocation.class::cast);
  }
}
