package latte.jdk21.javax.lang.model;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.SourceVersion")
public interface SourceVersionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 22")
  int ordinal();
}
