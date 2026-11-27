package latte.jdk21.org.w3c.dom;

import liquidjava.specification.*;
import specification.*;

/**
 * Length is a nonnegative snapshot of a possibly live map; nodes and views are not fresh.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/w3c/dom/NamedNodeMap.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/w3c/dom/NamedNodeMap.java
 */
@ExternalRefinementsFor("org.w3c.dom.NamedNodeMap")
public interface NamedNodeMapSpec {
  @Refinement("_ >= 0")
  int getLength();
}
