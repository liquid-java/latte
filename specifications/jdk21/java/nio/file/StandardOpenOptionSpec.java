package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.StandardOpenOption")
public interface StandardOpenOptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 10")
  int ordinal();
}
