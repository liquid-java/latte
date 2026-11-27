package latte.jdk21.java.lang.reflect;

import liquidjava.specification.*;
import specification.*;

/**
 * Reflection reads the array length without retaining the array. A non-array argument still throws;
 * no array-content predicate is inferred. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Array.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/reflect/Array.java
 */
@ExternalRefinementsFor("java.lang.reflect.Array")
public interface ArraySpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getLength(@Borrowed @Refinement("_ != null") Object array);
}
