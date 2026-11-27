package latte.jdk21.java.lang.module;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.module.ModuleDescriptor$Modifier")
public interface ModuleDescriptor_ModifierSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
