package latte.jdk21.java.time.chrono;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.chrono.HijrahEra")
public interface HijrahEraSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 1")
  int ordinal();
}
