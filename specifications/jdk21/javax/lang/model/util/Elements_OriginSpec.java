package latte.jdk21.javax.lang.model.util;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.util.Elements$Origin")
public interface Elements_OriginSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
