package latte.jdk21.java.math;

import liquidjava.specification.*;
import specification.*;

/**
 * Precision is positive, including zero values. Scale may be negative and is intentionally
 * unconstrained. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/math/BigDecimal.java
 */
@ExternalRefinementsFor("java.math.BigDecimal")
public interface BigDecimalSpec {
  @Refinement("_ >= -1 && _ <= 1")
  int signum();

  @Refinement("_ >= 1")
  int precision();
}
