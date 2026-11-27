package latte.jdk21.java.time.chrono;

import liquidjava.specification.*;
import specification.*;

/**
 * Calendar-dependent lengths are positive; ISO month/year upper bounds are not imposed on other
 * chronologies. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/chrono/ChronoLocalDate.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/chrono/ChronoLocalDate.java
 */
@ExternalRefinementsFor("java.time.chrono.ChronoLocalDate")
public interface ChronoLocalDateSpec {
  @Refinement("_ >= 1")
  int lengthOfMonth();

  @Refinement("_ >= 1")
  int lengthOfYear();
}
