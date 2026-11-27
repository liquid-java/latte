package latte.jdk21.java.security;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.DrbgParameters$Capability")
public interface DrbgParameters_CapabilitySpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
