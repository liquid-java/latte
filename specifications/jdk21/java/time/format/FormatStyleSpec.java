package latte.jdk21.java.time.format;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.format.FormatStyle")
public interface FormatStyleSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
