package latte.jdk21.java.time.format;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.format.ResolverStyle")
public interface ResolverStyleSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
