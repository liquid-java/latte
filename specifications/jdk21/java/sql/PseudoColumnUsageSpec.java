package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.sql.PseudoColumnUsage")
public interface PseudoColumnUsageSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
