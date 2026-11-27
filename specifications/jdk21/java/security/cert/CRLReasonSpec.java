package latte.jdk21.java.security.cert;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.cert.CRLReason")
public interface CRLReasonSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 11")
  int ordinal();
}
