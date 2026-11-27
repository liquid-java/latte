package latte.jdk21.javax.swing;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.LayoutStyle$ComponentPlacement")
public interface LayoutStyle_ComponentPlacementSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
