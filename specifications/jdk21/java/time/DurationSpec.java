package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * The nanosecond adjustment stays nonnegative even for negative durations; seconds may be negative.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/Duration.java
 */
@ExternalRefinementsFor("java.time.Duration")
public interface DurationSpec {
  @Modifies({})
  @Refinement("_ >= 0 && _ <= 999999999")
  int getNano();
}
