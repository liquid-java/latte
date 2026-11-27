package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.StandardCopyOption")
public interface StandardCopyOptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
