package latte.jdk21.java.time.temporal;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.temporal.ChronoField")
public interface ChronoFieldSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 30")
  int ordinal();
}
