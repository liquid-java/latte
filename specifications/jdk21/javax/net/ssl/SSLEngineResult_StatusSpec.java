package latte.jdk21.javax.net.ssl;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.net.ssl.SSLEngineResult$Status")
public interface SSLEngineResult_StatusSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
