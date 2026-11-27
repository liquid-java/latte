package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * Nanosecond adjustment is normalized independently of signed epoch seconds. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/Instant.java
 */
@ExternalRefinementsFor("java.time.Instant")
public interface InstantSpec {
  @Modifies({})
  @Refinement("_ >= 0 && _ <= 999999999")
  int getNano();
}
