package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.Locale$FilteringMode")
public interface Locale_FilteringModeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
