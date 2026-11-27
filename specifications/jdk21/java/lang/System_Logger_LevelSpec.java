package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.System$Logger$Level")
public interface System_Logger_LevelSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 7")
  int ordinal();
}
