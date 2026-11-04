package latte.analysis;

import java.util.Map;
import java.util.Optional;
import spoon.reflect.declaration.CtElement;

/**
 * Ownership transfers at construction: new owner identities and their consumed caller identities.
 */
public record ConstructionFacts(Map<String, String> transferredFrom) {
  public static final String KEY = "latte.analysis.construction.v1";

  public ConstructionFacts {
    transferredFrom = Map.copyOf(transferredFrom);
  }

  public static Optional<ConstructionFacts> on(CtElement element) {
    return Optional.ofNullable(element.getMetadata(KEY))
        .filter(ConstructionFacts.class::isInstance)
        .map(ConstructionFacts.class::cast);
  }

  public void attach(CtElement element) {
    element.putMetadata(KEY, this);
  }
}
