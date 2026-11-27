package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * Time-of-day fields and totals use exclusive next-day upper bounds. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalTime.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/LocalTime.java
 */
@ExternalRefinementsFor("java.time.LocalTime")
public interface LocalTimeSpec {
  @Modifies({})
  @Refinement("_ >= 0 && _ <= 23")
  int getHour();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 59")
  int getMinute();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 59")
  int getSecond();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 999999999")
  int getNano();

  @Modifies({})
  @Refinement("_ >= 0 && _ < 86400")
  int toSecondOfDay();

  @Modifies({})
  @Refinement("_ >= 0 && _ < 86400000000000")
  long toNanoOfDay();
}
