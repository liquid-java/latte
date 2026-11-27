package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * IEEE NaN and infinity classification. Comparison bounds include signed zero and NaN ordering
 * without equating them to ordinary numeric comparison. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Float.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Float.java
 */
@ExternalRefinementsFor("java.lang.Float")
public interface FloatSpec {
  @Modifies({})
  @Refinement("_ == (value != value)")
  boolean isNaN(float value);

  @Modifies({})
  @Refinement("_ == (value == Float.POSITIVE_INFINITY || value == Float.NEGATIVE_INFINITY)")
  boolean isInfinite(float value);

  @Modifies({})
  @Refinement(
      "_ == (value == value && value != Float.POSITIVE_INFINITY && value !="
          + " Float.NEGATIVE_INFINITY)")
  boolean isFinite(float value);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= 1")
  int compare(float x, float y);
}
