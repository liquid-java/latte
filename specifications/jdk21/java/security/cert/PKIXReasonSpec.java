package latte.jdk21.java.security.cert;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.cert.PKIXReason")
public interface PKIXReasonSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 8")
  int ordinal();
}
