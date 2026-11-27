package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.StackWalker$Option")
public interface StackWalker_OptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
