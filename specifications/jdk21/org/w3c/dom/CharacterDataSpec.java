package latte.jdk21.org.w3c.dom;

import liquidjava.specification.*;
import specification.*;

/**
 * Character-data length is a nonnegative UTF-16 code-unit count; mutable nodes remain shared unless
 * locally controlled. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/w3c/dom/CharacterData.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/w3c/dom/CharacterData.java
 */
@ExternalRefinementsFor("org.w3c.dom.CharacterData")
public interface CharacterDataSpec {
  @Refinement("_ >= 0")
  int getLength();
}
