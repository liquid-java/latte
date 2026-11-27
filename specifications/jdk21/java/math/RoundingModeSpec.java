package latte.jdk21.java.math;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.math.RoundingMode")
public interface RoundingModeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 8")
  int ordinal();
}
