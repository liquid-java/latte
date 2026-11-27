package latte.jdk21.javax.xml.stream;

import liquidjava.specification.*;
import specification.*;

/**
 * Counts are nonnegative on permitted events; invalid reader states may throw. The event protocol
 * and retained input are not modeled. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/stream/XMLStreamReader.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/javax/xml/stream/XMLStreamReader.java
 */
@ExternalRefinementsFor("javax.xml.stream.XMLStreamReader")
public interface XMLStreamReaderSpec {
  @Refinement("_ >= 0")
  int getAttributeCount();

  @Refinement("_ >= 0")
  int getNamespaceCount();
}
