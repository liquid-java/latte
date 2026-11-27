package latte.jdk21.java.time;

import liquidjava.specification.*;
import specification.*;

/**
 * Proleptic year range and leap-year length. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Year.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/time/Year.java
 */
@ExternalRefinementsFor("java.time.Year")
public interface YearSpec {
  @Modifies({})
  @Refinement("_ >= -999999999 && _ <= 999999999")
  int getValue();

  @Modifies({})
  @Refinement("_ == 365 || _ == 366")
  int length();
}
