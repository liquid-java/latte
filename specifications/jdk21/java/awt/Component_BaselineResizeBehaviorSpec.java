package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.Component$BaselineResizeBehavior")
public interface Component_BaselineResizeBehaviorSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
