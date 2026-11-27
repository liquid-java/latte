package latte.jdk21.java.security;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.KeyRep$Type")
public interface KeyRep_TypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
