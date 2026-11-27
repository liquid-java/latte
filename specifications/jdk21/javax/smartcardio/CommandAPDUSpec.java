package latte.jdk21.javax.smartcardio;

import liquidjava.specification.*;
import specification.*;

/**
 * APDU parsing checks the encoded short/extended lengths and copies byte-array input. Accessors
 * return unsigned header fields; getBytes/getData allocate copies. This does not model card
 * communication. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.smartcardio/javax/smartcardio/CommandAPDU.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.smartcardio/share/classes/javax/smartcardio/CommandAPDU.java
 */
@ExternalRefinementsFor("javax.smartcardio.CommandAPDU")
public interface CommandAPDUSpec {
  @Modifies({})
  void CommandAPDU(@Borrowed @Refinement("_ != null && length(_) >= 4") byte[] bytes);

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getCLA();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getINS();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getP1();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int getP2();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 65535")
  int getNc();

  @Modifies({})
  @Refinement("_ >= 0 && _ <= 65536")
  int getNe();

  @Modifies({})
  @Refinement("_ != null && length(_) >= 4")
  @Free
  byte[] getBytes();

  @Modifies({})
  @Refinement("_ != null")
  @Free
  byte[] getData();
}
