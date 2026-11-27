package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Signed comparison, sign and bit-count bounds. Cached boxing factories are deliberately not
 * annotated Free. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Long.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Long.java
 */
@ExternalRefinementsFor("java.lang.Long")
public interface LongSpec {
  @Modifies({})
  @Refinement("_ == (x < y ? -1 : (x == y ? 0 : 1))")
  int compare(long x, long y);

  @Modifies({})
  @Refinement("_ == (x < 0 ? -1 : (x == 0 ? 0 : 1))")
  int signum(long x);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 64")
  int bitCount(long value);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 64")
  int numberOfLeadingZeros(long value);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 64")
  int numberOfTrailingZeros(long value);
}
