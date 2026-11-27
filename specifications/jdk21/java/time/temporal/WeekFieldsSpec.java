package latte.jdk21.java.time.temporal;

import liquidjava.specification.*;
import specification.*;

/**
 * First-week minimum is between one and seven days. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/WeekFields.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/temporal/WeekFields.java
 */
@ExternalRefinementsFor("java.time.temporal.WeekFields")
public interface WeekFieldsSpec {
  @Modifies({})
  @Refinement("_ >= 1 && _ <= 7")
  int getMinimalDaysInFirstWeek();
}
