package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * Month numbering and month lengths; February depends on the supplied leap-year flag.
 * Documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Month.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/Month.java
 */
@ExternalRefinementsFor("java.time.Month")
public interface MonthSpec {
  @Modifies({})
  @Refinement("_ >= 1 && _ <= 12")
  int getValue();

  @Modifies({})
  @Refinement("_ >= 28 && _ <= 31")
  int minLength();

  @Modifies({})
  @Refinement("_ >= 29 && _ <= 31")
  int maxLength();

  @Modifies({})
  @Refinement("_ >= 28 && _ <= 31")
  int length(boolean leapYear);

  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 12")
  int ordinal();
}
