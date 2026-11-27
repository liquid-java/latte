package latte.jdk21.java.security.cert;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.security.cert.PKIXRevocationChecker$Option")
public interface PKIXRevocationChecker_OptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
