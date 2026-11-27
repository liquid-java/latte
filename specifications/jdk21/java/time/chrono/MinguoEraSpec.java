package latte.jdk21.java.time.chrono;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.chrono.MinguoEra")
public interface MinguoEraSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
