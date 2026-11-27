package latte.jdk21.java.lang.module;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.module.ModuleDescriptor$Requires$Modifier")
public interface ModuleDescriptor_Requires_ModifierSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
