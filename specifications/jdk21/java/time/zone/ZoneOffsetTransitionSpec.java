package latte.jdk21.java.time.zone;

import liquidjava.specification.*;
import specification.*;

/**
 * Comparison delegates to Long.compare on epoch seconds; the comparison argument is conservatively
 * shared. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneOffsetTransition.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/zone/ZoneOffsetTransition.java
 */
@ExternalRefinementsFor("java.time.zone.ZoneOffsetTransition")
public interface ZoneOffsetTransitionSpec {
  @Modifies({})
  @Refinement("_ >= -1 && _ <= 1")
  int compareTo(@Borrowed java.time.zone.ZoneOffsetTransition other);
}
