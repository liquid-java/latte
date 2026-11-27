package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.FileVisitResult")
public interface FileVisitResultSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
