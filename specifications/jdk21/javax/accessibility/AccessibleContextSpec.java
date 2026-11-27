package latte.jdk21.javax.accessibility;

import liquidjava.specification.*;
import specification.*;

/**
 * The accessibility provider must return a nonnegative child count. No GUI-thread state stability
 * or ownership of child components is implied. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/accessibility/AccessibleContext.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/accessibility/AccessibleContext.java
 */
@ExternalRefinementsFor("javax.accessibility.AccessibleContext")
public interface AccessibleContextSpec {
  @Refinement("_ >= 0")
  int getAccessibleChildrenCount();
}
