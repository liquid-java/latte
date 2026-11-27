package latte.jdk21.java.security;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.CryptoPrimitive")
public interface CryptoPrimitiveSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 10")
  int ordinal();
}
