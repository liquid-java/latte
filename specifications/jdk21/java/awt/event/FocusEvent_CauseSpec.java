package latte.jdk21.java.awt.event;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.event.FocusEvent$Cause")
public interface FocusEvent_CauseSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 11")
  int ordinal();
}
