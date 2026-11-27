package latte.jdk21.java.math;

import liquidjava.specification.*;
import specification.*;

/**
 * Scalar bounds include zero and negative integers. Internally cached values are not marked Free.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/math/BigInteger.java
 */
@ExternalRefinementsFor("java.math.BigInteger")
public interface BigIntegerSpec {
  @Modifies({})
  @Refinement("_ >= -1 && _ <= 1")
  int signum();

  @Modifies("this.*")
  @Refinement("_ >= 0")
  int bitLength();

  @Modifies("this.*")
  @Refinement("_ >= 0")
  int bitCount();

  @Modifies("this.*")
  @Refinement("_ >= -1")
  int getLowestSetBit();
}
