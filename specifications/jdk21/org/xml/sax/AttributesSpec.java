package latte.jdk21.org.xml.sax;

import liquidjava.specification.*;
import specification.*;

/**
 * Attribute count follows the SAX interface contract. Parser-owned attributes and their lifetimes
 * are not made unique. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/xml/sax/Attributes.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/xml/sax/Attributes.java
 */
@ExternalRefinementsFor("org.xml.sax.Attributes")
public interface AttributesSpec {
  @Refinement("_ >= 0")
  int getLength();
}
