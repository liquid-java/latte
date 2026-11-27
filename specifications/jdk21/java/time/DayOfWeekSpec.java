package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * ISO day-of-week numbering starts at Monday=1. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/DayOfWeek.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/DayOfWeek.java
 */
@ExternalRefinementsFor("java.time.DayOfWeek")
public interface DayOfWeekSpec {
  @Modifies({})
  @Refinement("_ >= 1 && _ <= 7")
  int getValue();

  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 7")
  int ordinal();
}
