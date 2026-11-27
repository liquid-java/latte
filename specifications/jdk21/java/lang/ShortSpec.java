package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Unsigned widening preserves the low bits; signed comparison uses an int difference without
 * overflow. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Short.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Short.java
 */
@ExternalRefinementsFor("java.lang.Short")
public interface ShortSpec {
  @Modifies({})
  @Refinement("_ == (value < 0 ? value + 65536 : value)")
  int toUnsignedInt(short value);

  @Modifies({})
  @Refinement("_ == (value < 0 ? value + 65536 : value)")
  long toUnsignedLong(short value);

  @Modifies({})
  @Refinement("_ == x - y")
  int compare(short x, short y);
}
