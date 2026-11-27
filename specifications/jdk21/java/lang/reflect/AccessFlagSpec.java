package latte.jdk21.java.lang.reflect;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.reflect.AccessFlag")
public interface AccessFlagSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 23")
  int ordinal();
}
