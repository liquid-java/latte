package latte.jdk21.java.lang.annotation;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.annotation.ElementType")
public interface ElementTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 12")
  int ordinal();
}
