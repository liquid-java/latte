package latte.jdk21.java.time.zone;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.time.zone.ZoneOffsetTransitionRule$TimeDefinition")
public interface ZoneOffsetTransitionRule_TimeDefinitionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
