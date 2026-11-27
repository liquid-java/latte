package latte.jdk21.java.nio.file.attribute;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.attribute.PosixFilePermission")
public interface PosixFilePermissionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 9")
  int ordinal();
}
