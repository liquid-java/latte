package latte.jdk21.java.lang.reflect;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.reflect.AccessFlag$Location")
public interface AccessFlag_LocationSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 9")
  int ordinal();
}
