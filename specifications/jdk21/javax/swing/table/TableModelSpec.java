package latte.jdk21.javax.swing.table;

import liquidjava.specification.*;
import specification.*;

/**
 * Counts follow the table-model interface contract. Listener retention, callbacks and event-thread
 * rules remain unspecified. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/table/TableModel.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/swing/table/TableModel.java
 */
@ExternalRefinementsFor("javax.swing.table.TableModel")
public interface TableModelSpec {
  @Refinement("_ >= 0")
  int getRowCount();

  @Refinement("_ >= 0")
  int getColumnCount();
}
