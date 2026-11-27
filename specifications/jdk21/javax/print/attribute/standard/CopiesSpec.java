package latte.jdk21.javax.print.attribute.standard;

import liquidjava.specification.*;
import specification.*;

/**
 * Copies validates its inherited integer value in the inclusive range 1 to Integer.MAX_VALUE. No
 * corresponding lower bound is imposed on arbitrary IntegerSyntax subclasses. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/print/attribute/standard/Copies.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/print/attribute/standard/Copies.java
 */
@ExternalRefinementsFor("javax.print.attribute.standard.Copies")
public interface CopiesSpec {
  @Modifies({})
  @Refinement("_ >= 1")
  int getValue();
}
