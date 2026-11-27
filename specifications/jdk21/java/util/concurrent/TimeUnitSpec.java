package latte.jdk21.java.util.concurrent;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.concurrent.TimeUnit")
public interface TimeUnitSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 7")
  int ordinal();
}
