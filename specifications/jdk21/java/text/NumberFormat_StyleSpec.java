package latte.jdk21.java.text;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.text.NumberFormat$Style")
public interface NumberFormat_StyleSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
