package latte.jdk21.java.awt.desktop;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.awt.desktop.UserSessionEvent$Reason")
public interface UserSessionEvent_ReasonSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
