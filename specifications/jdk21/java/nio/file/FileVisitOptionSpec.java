package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.FileVisitOption")
public interface FileVisitOptionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 1")
  int ordinal();
}
