package latte.jdk21.java.lang.invoke;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.invoke.MethodHandles$Lookup$ClassOption")
public interface MethodHandles_Lookup_ClassOptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
