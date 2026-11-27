package latte.jdk21.java.lang.invoke;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.invoke.VarHandle$AccessMode")
public interface VarHandle_AccessModeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 31")
  int ordinal();
}
