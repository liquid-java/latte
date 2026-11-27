package latte.jdk21.javax.lang.model.type;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.type.TypeKind")
public interface TypeKindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 22")
  int ordinal();
}
