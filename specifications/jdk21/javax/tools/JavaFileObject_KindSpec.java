package latte.jdk21.javax.tools;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.tools.JavaFileObject$Kind")
public interface JavaFileObject_KindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
