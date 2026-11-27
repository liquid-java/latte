package latte.jdk21.javax.tools;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.tools.StandardLocation")
public interface StandardLocationSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 13")
  int ordinal();
}
