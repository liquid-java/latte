package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.sql.JDBCType")
public interface JDBCTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 39")
  int ordinal();
}
