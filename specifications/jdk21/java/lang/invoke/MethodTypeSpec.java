package latte.jdk21.java.lang.invoke;

import liquidjava.specification.*;
import specification.*;

/**
 * Parameter slots are checked against the JVM limit of 255; long/double consume two slots. Interned
 * method types are not fresh. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/invoke/MethodType.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/invoke/MethodType.java
 */
@ExternalRefinementsFor("java.lang.invoke.MethodType")
public interface MethodTypeSpec {
  @Modifies({})
  @Refinement("_ >= 0 && _ <= 255")
  int parameterCount();
}
