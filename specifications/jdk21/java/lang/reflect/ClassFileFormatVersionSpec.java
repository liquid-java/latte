package latte.jdk21.java.lang.reflect;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.reflect.ClassFileFormatVersion")
public interface ClassFileFormatVersionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 22")
  int ordinal();
}
