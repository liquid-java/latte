package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.sql.RowIdLifetime")
public interface RowIdLifetimeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 5")
  int ordinal();
}
