package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.Desktop$Action")
public interface Desktop_ActionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 24")
  int ordinal();
}
