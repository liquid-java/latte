package latte.jdk21.java.awt.print;

import liquidjava.specification.*;
import specification.*;

/**
 * Page count is the backing vector size. Printable callbacks and print-job state are not specified.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/print/Book.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/java/awt/print/Book.java
 */
@ExternalRefinementsFor("java.awt.print.Book")
public interface BookSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getNumberOfPages();
}
