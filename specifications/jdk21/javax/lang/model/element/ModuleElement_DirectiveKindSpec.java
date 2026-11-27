package latte.jdk21.javax.lang.model.element;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.element.ModuleElement$DirectiveKind")
public interface ModuleElement_DirectiveKindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
