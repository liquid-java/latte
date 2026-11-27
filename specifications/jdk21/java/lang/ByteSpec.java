package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Unsigned widening preserves the low bits; signed comparison uses an int difference without
 * overflow. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Byte.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Byte.java
 */
@ExternalRefinementsFor("java.lang.Byte")
public interface ByteSpec {
  @Modifies({})
  @Refinement("_ == (value < 0 ? value + 256 : value)")
  int toUnsignedInt(byte value);

  @Modifies({})
  @Refinement("_ == (value < 0 ? value + 256 : value)")
  long toUnsignedLong(byte value);

  @Modifies({})
  @Refinement("_ == x - y")
  int compare(byte x, byte y);
}
