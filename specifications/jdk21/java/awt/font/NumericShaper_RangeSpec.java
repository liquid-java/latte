package latte.jdk21.java.awt.font;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.font.NumericShaper$Range")
public interface NumericShaper_RangeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 37")
  int ordinal();
}
