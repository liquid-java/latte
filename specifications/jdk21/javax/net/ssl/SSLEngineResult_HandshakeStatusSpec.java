package latte.jdk21.javax.net.ssl;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.net.ssl.SSLEngineResult$HandshakeStatus")
public interface SSLEngineResult_HandshakeStatusSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 6")
  int ordinal();
}
