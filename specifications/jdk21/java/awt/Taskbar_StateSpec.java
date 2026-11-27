package latte.jdk21.java.awt;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.Taskbar$State")
public interface Taskbar_StateSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
