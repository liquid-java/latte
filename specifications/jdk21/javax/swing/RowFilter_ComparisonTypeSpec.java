package latte.jdk21.javax.swing;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.RowFilter$ComparisonType")
public interface RowFilter_ComparisonTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
