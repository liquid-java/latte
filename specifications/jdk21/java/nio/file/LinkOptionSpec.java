package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.LinkOption")
public interface LinkOptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 1")
  int ordinal();
}
