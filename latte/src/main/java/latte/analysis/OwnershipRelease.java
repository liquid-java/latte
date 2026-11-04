package latte.analysis;

import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtElement;

/** Forgotten ownership edges from unobservable owners; these are not Java writes or null facts. */
public record OwnershipRelease(Set<FlowState.Field> fields) {
  public static final String KEY = "latte.analysis.release.v1";

  public OwnershipRelease {
    fields = Set.copyOf(fields);
  }

  public static Optional<OwnershipRelease> on(CtElement element) {
    return Optional.ofNullable(element.getMetadata(KEY))
        .filter(OwnershipRelease.class::isInstance)
        .map(OwnershipRelease.class::cast);
  }

  public void attach(CtElement element) {
    element.putMetadata(KEY, this);
  }
}
