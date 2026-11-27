package latte.jdk21.javax.swing;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.DropMode")
public interface DropModeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 8")
  int ordinal();
}
