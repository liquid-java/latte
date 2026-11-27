package latte.jdk21.java.time.temporal;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.temporal.ChronoUnit")
public interface ChronoUnitSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 16")
  int ordinal();
}
