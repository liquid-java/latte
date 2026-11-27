package latte.jdk21.java.lang.annotation;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.lang.annotation.RetentionPolicy")
public interface RetentionPolicySpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
