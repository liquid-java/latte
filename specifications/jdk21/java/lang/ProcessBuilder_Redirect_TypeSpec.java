package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.ProcessBuilder$Redirect$Type")
public interface ProcessBuilder_Redirect_TypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
