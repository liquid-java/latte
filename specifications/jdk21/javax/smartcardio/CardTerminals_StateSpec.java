package latte.jdk21.javax.smartcardio;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.smartcardio.CardTerminals$State")
public interface CardTerminals_StateSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
