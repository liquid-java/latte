package latte.jdk21.javax.swing;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.SortOrder")
public interface SortOrderSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
