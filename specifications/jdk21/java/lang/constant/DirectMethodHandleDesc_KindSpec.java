package latte.jdk21.java.lang.constant;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.constant.DirectMethodHandleDesc$Kind")
public interface DirectMethodHandleDesc_KindSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 11")
  int ordinal();
}
