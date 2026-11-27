package latte.jdk21.java.sql;

import liquidjava.specification.*;
import specification.*;

/**
 * Column count only; provider-dependent precision, scale, SQL values and nullability are not
 * approximated. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/ResultSetMetaData.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.sql/share/classes/java/sql/ResultSetMetaData.java
 */
@ExternalRefinementsFor("java.sql.ResultSetMetaData")
public interface ResultSetMetaDataSpec {
  @Refinement("_ >= 0")
  int getColumnCount();
}
