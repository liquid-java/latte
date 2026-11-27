package latte.jdk21.java.time.format;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.format.SignStyle")
public interface SignStyleSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
