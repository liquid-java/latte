package latte.jdk21.javax.naming.directory;

import liquidjava.specification.*;
import specification.*;

/**
 * Attribute value count only. Implementations may retain inserted values; neither elements nor
 * directory state are made unique. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.naming/javax/naming/directory/Attribute.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.naming/share/classes/javax/naming/directory/Attribute.java
 */
@ExternalRefinementsFor("javax.naming.directory.Attribute")
public interface AttributeSpec {
  @Refinement("_ >= 0")
  int size();
}
