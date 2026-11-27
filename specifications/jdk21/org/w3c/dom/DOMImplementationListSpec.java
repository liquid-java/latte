package latte.jdk21.org.w3c.dom;

import liquidjava.specification.*;
import specification.*;

/**
 * Number of DOM implementations in the list; implementations themselves remain shared provider
 * objects. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/w3c/dom/DOMImplementationList.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/w3c/dom/DOMImplementationList.java
 */
@ExternalRefinementsFor("org.w3c.dom.DOMImplementationList")
public interface DOMImplementationListSpec {
  @Refinement("_ >= 0")
  int getLength();
}
