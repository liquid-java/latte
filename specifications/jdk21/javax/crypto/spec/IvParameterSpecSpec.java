package latte.jdk21.javax.crypto.spec;

import liquidjava.specification.*;
import specification.*;

/**
 * Constructor input and getIV output are copied; no cipher-specific IV length is assumed.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/spec/IvParameterSpec.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/javax/crypto/spec/IvParameterSpec.java
 */
@ExternalRefinementsFor("javax.crypto.spec.IvParameterSpec")
public interface IvParameterSpecSpec {
  @Modifies({})
  void IvParameterSpec(@Borrowed @Refinement("_ != null") byte[] iv);

  @Modifies({})
  @Free
  @Refinement("_ != null")
  byte[] getIV();
}
