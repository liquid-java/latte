package latte.jdk21.java.time.chrono;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.chrono.IsoEra")
public interface IsoEraSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
