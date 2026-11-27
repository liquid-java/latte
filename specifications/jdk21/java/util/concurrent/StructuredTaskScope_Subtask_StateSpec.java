package latte.jdk21.java.util.concurrent;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.concurrent.StructuredTaskScope$Subtask$State")
public interface StructuredTaskScope_Subtask_StateSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
