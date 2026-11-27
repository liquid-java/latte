package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.Character$UnicodeScript")
public interface Character_UnicodeScriptSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 164")
  int ordinal();
}
