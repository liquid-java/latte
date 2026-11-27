package latte.jdk21.javax.tools;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.tools.Diagnostic$Kind")
public interface Diagnostic_KindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
