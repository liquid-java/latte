package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.Locale$IsoCountryCode")
public interface Locale_IsoCountryCodeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
