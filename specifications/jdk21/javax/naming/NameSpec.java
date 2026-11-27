package latte.jdk21.javax.naming;

import liquidjava.specification.*;
import specification.*;

/**
 * Name component count follows the naming interface contract. Lookup results and namespace state
 * are not owned by the caller. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.naming/javax/naming/Name.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.naming/share/classes/javax/naming/Name.java
 */
@ExternalRefinementsFor("javax.naming.Name")
public interface NameSpec {
  @Refinement("_ >= 0")
  int size();
}
