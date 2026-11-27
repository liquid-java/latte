package latte.jdk21.org.w3c.dom;

import liquidjava.specification.*;
import specification.*;

/**
 * Length is the nonnegative current node count. Live views can change externally, so no receiver
 * state or fresh element ownership is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/w3c/dom/NodeList.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/w3c/dom/NodeList.java
 */
@ExternalRefinementsFor("org.w3c.dom.NodeList")
public interface NodeListSpec {
  @Refinement("_ >= 0")
  int getLength();
}
