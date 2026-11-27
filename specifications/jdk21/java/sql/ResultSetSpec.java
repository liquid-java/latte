package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

/**
 * JDBC row zero denotes no current row. Fetch size is nonnegative. Driver implementations are
 * trusted to honor JDBC; cursor and transaction state are not modeled. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/ResultSet.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.sql/share/classes/java/sql/ResultSet.java
 */
@ExternalRefinementsFor("java.sql.ResultSet")
public interface ResultSetSpec {
  @Refinement("_ >= 0")
  int getRow();

  @Refinement("_ >= 0")
  int getFetchSize();
}
