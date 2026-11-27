package latte.jdk21.java.lang.management;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.management.MemoryType")
public interface MemoryTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
