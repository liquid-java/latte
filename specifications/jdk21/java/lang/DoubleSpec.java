package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * IEEE NaN and infinity classification. Comparison bounds include signed zero and NaN ordering
 * without equating them to ordinary numeric comparison. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Double.java
 */
@ExternalRefinementsFor("java.lang.Double")
public interface DoubleSpec {
  @Modifies({})
  @Refinement("_ == (value != value)")
  boolean isNaN(double value);

  @Modifies({})
  @Refinement("_ == (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY)")
  boolean isInfinite(double value);

  @Modifies({})
  @Refinement(
      "_ == (value == value && value != Double.POSITIVE_INFINITY && value !="
          + " Double.NEGATIVE_INFINITY)")
  boolean isFinite(double value);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= 1")
  int compare(double x, double y);
}
