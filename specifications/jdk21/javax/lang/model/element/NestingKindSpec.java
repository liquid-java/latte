package latte.jdk21.javax.lang.model.element;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.element.NestingKind")
public interface NestingKindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
