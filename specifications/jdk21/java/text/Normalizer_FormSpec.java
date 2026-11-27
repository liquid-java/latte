package latte.jdk21.java.text;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.text.Normalizer$Form")
public interface Normalizer_FormSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
