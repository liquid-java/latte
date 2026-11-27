package latte.jdk21.java.awt.desktop;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.desktop.QuitStrategy")
public interface QuitStrategySpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
