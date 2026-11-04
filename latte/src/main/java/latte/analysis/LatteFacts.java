package latte.analysis;

import java.util.Optional;
import spoon.reflect.declaration.CtElement;

/**
 * The sole handoff between the ownership pass and LiquidTypeChecking. Stored on each evaluated
 * Spoon node.
 */
public record LatteFacts(FlowState before, FlowState after, Reference value) {
  public static final String KEY = "latte.analysis.facts.v1";

  public Permission permission() {
    return after.permission(value);
  }

  public boolean refinable() {
    return permission().refinable();
  }

  public static Optional<LatteFacts> on(CtElement element) {
    if (element == null) return Optional.empty();
    return Optional.ofNullable(element.getMetadata(KEY))
        .filter(LatteFacts.class::isInstance)
        .map(LatteFacts.class::cast);
  }

  public void attach(CtElement element) {
    element.putMetadata(KEY, this);
  }
}
