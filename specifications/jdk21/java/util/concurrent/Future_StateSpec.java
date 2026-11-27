package latte.jdk21.java.util.concurrent;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.concurrent.Future$State")
public interface Future_StateSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
