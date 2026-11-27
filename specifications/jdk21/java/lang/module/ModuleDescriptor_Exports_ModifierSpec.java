package latte.jdk21.java.lang.module;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.module.ModuleDescriptor$Exports$Modifier")
public interface ModuleDescriptor_Exports_ModifierSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
