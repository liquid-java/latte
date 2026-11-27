package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.Thread$State")
public interface Thread_StateSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 6")
  int ordinal();
}
