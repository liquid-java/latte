package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * ISO year/month observer bounds, without assuming factory allocation. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/YearMonth.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/YearMonth.java
 */
@ExternalRefinementsFor("java.time.YearMonth")
public interface YearMonthSpec {
  @Modifies({})
  @Refinement("_ >= -999999999 && _ <= 999999999")
  int getYear();

  @Modifies({})
  @Refinement("_ >= 1 && _ <= 12")
  int getMonthValue();

  @Modifies({})
  @Refinement("_ >= 28 && _ <= 31")
  int lengthOfMonth();

  @Modifies({})
  @Refinement("_ == 365 || _ == 366")
  int lengthOfYear();
}
