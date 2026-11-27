package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

/**
 * Update count uses -1 when no update count is available; fetch size and timeout are nonnegative.
 * Zero timeout means unlimited. Batch sentinel values do not apply to getUpdateCount.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/Statement.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.sql/share/classes/java/sql/Statement.java
 */
@ExternalRefinementsFor("java.sql.Statement")
public interface StatementSpec {
  @Refinement("_ >= -1")
  int getUpdateCount();

  @Refinement("_ >= 0")
  int getFetchSize();

  @Refinement("_ >= 0")
  int getQueryTimeout();
}
