package latte.jdk21.javax.lang.model.element;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.element.ElementKind")
public interface ElementKindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 21")
  int ordinal();
}
