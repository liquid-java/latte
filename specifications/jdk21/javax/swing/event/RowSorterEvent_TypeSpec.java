package latte.jdk21.javax.swing.event;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.event.RowSorterEvent$Type")
public interface RowSorterEvent_TypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
