package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.Locale$Category")
public interface Locale_CategorySpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
