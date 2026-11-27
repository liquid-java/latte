package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.MultipleGradientPaint$ColorSpaceType")
public interface MultipleGradientPaint_ColorSpaceTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
