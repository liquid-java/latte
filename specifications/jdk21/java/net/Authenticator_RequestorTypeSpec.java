package latte.jdk21.java.net;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.net.Authenticator$RequestorType")
public interface Authenticator_RequestorTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
