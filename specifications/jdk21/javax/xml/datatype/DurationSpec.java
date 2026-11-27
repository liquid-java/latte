package latte.jdk21.javax.xml.datatype;

import liquidjava.specification.*;
import specification.*;

/**
 * Sign is -1, 0 or 1. Component getters narrow arbitrary-precision fields to int and therefore do
 * not receive nonnegative bounds. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/datatype/Duration.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/javax/xml/datatype/Duration.java
 */
@ExternalRefinementsFor("javax.xml.datatype.Duration")
public interface DurationSpec {
  @Refinement("_ >= -1 && _ <= 1")
  int getSign();
}
