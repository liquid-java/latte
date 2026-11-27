package latte.jdk21.java.net;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.net.Proxy$Type")
public interface Proxy_TypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
