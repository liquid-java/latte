package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

/**
 * Channels are masked to unsigned eight-bit values. No identity, color-space equivalence or
 * constructor caching assumption is made. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Color.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/java/awt/Color.java
 */
@ExternalRefinementsFor("java.awt.Color")
public interface ColorSpec {
  @Refinement("_ >= 0 && _ <= 255")
  int getRed();

  @Refinement("_ >= 0 && _ <= 255")
  int getGreen();

  @Refinement("_ >= 0 && _ <= 255")
  int getBlue();

  @Refinement("_ >= 0 && _ <= 255")
  int getAlpha();
}
