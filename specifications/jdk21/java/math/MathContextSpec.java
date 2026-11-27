package latte.jdk21.java.math;

import liquidjava.specification.*;
import specification.*;

/**
 * Precision zero denotes unlimited precision. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/MathContext.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/math/MathContext.java
 */
@ExternalRefinementsFor("java.math.MathContext")
public interface MathContextSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getPrecision();
}
