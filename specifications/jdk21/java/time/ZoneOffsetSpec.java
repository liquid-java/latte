package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * Offsets range from minus to plus eighteen hours; cached offsets remain shared. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneOffset.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/ZoneOffset.java
 */
@ExternalRefinementsFor("java.time.ZoneOffset")
public interface ZoneOffsetSpec {
  @Modifies({})
  @Refinement("_ >= -64800 && _ <= 64800")
  int getTotalSeconds();
}
