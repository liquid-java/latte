package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Primitive boolean comparison and the documented hash values; cached Boolean objects remain
 * shared. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Boolean.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Boolean.java
 */
@ExternalRefinementsFor("java.lang.Boolean")
public interface BooleanSpec {
  @Modifies({})
  @Refinement("_ == (x == y ? 0 : (x ? 1 : -1))")
  int compare(boolean x, boolean y);

  @Modifies({})
  @Refinement("_ == (value ? 1231 : 1237)")
  int hashCode(boolean value);
}
