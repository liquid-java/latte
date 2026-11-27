package latte.jdk21.javax.sql.rowset.serial;

import liquidjava.specification.*;
import specification.*;

/**
 * Normal length reports the copied byte/character count. Access after free may throw; no external
 * database state is inferred. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.sql.rowset/javax/sql/rowset/serial/SerialBlob.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.sql.rowset/share/classes/javax/sql/rowset/serial/SerialBlob.java
 */
@ExternalRefinementsFor("javax.sql.rowset.serial.SerialBlob")
public interface SerialBlobSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  long length();
}
