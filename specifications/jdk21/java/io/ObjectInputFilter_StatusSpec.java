package latte.jdk21.java.io;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.io.ObjectInputFilter$Status")
public interface ObjectInputFilter_StatusSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
