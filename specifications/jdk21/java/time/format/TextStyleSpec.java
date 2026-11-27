package latte.jdk21.java.time.format;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.format.TextStyle")
public interface TextStyleSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 6")
  int ordinal();
}
