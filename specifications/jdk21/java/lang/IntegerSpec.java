package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Signed comparison, sign and bit-count bounds. Cached boxing factories are deliberately not
 * annotated Free. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Integer.java
 */
@ExternalRefinementsFor("java.lang.Integer")
public interface IntegerSpec {
  @Modifies({})
  @Refinement("_ == (x < y ? -1 : (x == y ? 0 : 1))")
  int compare(int x, int y);

  @Modifies({})
  @Refinement("_ == (x < 0 ? -1 : (x == 0 ? 0 : 1))")
  int signum(int x);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 32")
  int bitCount(int value);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 32")
  int numberOfLeadingZeros(int value);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 32")
  int numberOfTrailingZeros(int value);

  @Modifies({})
  @Refinement("_ == (value < 0 ? value + 4294967296 : value)")
  long toUnsignedLong(int value);
}
