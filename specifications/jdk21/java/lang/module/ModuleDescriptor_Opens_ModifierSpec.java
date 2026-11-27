package latte.jdk21.java.lang.module;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.module.ModuleDescriptor$Opens$Modifier")
public interface ModuleDescriptor_Opens_ModifierSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
