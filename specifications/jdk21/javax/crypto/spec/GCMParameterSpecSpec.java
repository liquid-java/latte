package latte.jdk21.javax.crypto.spec;

import liquidjava.specification.*;
import specification.*;

/**
 * The constructor rejects negative tag lengths and copies the IV; getIV returns a fresh clone.
 * Supported cryptographic tag sizes are provider-dependent and are not implied by construction.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/spec/GCMParameterSpec.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/javax/crypto/spec/GCMParameterSpec.java
 */
@ExternalRefinementsFor("javax.crypto.spec.GCMParameterSpec")
public interface GCMParameterSpecSpec {
  @Modifies({})
  void GCMParameterSpec(
      @Refinement("_ >= 0") int tagLength, @Borrowed @Refinement("_ != null") byte[] iv);

  @Modifies({})
  @Refinement("_ >= 0")
  int getTLen();

  @Modifies({})
  @Free
  @Refinement("_ != null")
  byte[] getIV();
}
