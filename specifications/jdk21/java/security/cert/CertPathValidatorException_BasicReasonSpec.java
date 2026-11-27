package latte.jdk21.java.security.cert;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.cert.CertPathValidatorException$BasicReason")
public interface CertPathValidatorException_BasicReasonSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 7")
  int ordinal();
}
