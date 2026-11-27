package latte.jdk21.javax.swing.text;

import liquidjava.specification.*;
import specification.*;

/**
 * Document length is a character count. Document listeners and positions may refer to shared
 * mutable text; no stable state is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/text/Document.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/swing/text/Document.java
 */
@ExternalRefinementsFor("javax.swing.text.Document")
public interface DocumentSpec {
  @Refinement("_ >= 0")
  int getLength();
}
