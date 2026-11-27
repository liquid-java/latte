package latte.jdk21.java.nio.charset;

import liquidjava.specification.*;
import specification.*;

/**
 * length returns a positive error span only on normal completion; non-error results throw
 * UnsupportedOperationException. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/CoderResult.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/nio/charset/CoderResult.java
 */
@ExternalRefinementsFor("java.nio.charset.CoderResult")
public interface CoderResultSpec {
  @Modifies({})
  @Refinement("_ > 0")
  int length();
}
