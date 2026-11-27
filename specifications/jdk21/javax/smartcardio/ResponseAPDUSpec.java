package latte.jdk21.javax.smartcardio;

import liquidjava.specification.*;
import specification.*;

/**
 * Response bytes are copied and must contain the two status bytes. Data length excludes status
 * bytes; returned arrays are fresh copies. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.smartcardio/javax/smartcardio/ResponseAPDU.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.smartcardio/share/classes/javax/smartcardio/ResponseAPDU.java
 */
@ExternalRefinementsFor("javax.smartcardio.ResponseAPDU")
public interface ResponseAPDUSpec {
  @Modifies({})
  void ResponseAPDU(@Borrowed @Refinement("_ != null && length(_) >= 2") byte[] bytes);

  @Modifies({})
  @Refinement("_ >= 0")
  int getNr();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 65535")
  int getSW();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getSW1();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getSW2();

  @Modifies({})
  @Refinement("_ != null && length(_) >= 2")
  @Free
  byte[] getBytes();

  @Modifies({})
  @Refinement("_ != null")
  @Free
  byte[] getData();
}
