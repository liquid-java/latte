package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.GraphicsDevice$WindowTranslucency")
public interface GraphicsDevice_WindowTranslucencySpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
