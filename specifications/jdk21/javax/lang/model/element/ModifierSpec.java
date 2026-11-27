package latte.jdk21.javax.lang.model.element;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.lang.model.element.Modifier")
public interface ModifierSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 14")
  int ordinal();
}
