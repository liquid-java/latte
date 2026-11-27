package latte.jdk21.javax.swing;

import liquidjava.specification.*;
import specification.*;

/**
 * The model interface promises an element count. Elements and registered listeners remain shared.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/ListModel.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/swing/ListModel.java
 */
@ExternalRefinementsFor("javax.swing.ListModel")
public interface ListModelSpec<E> {
  @Refinement("_ >= 0")
  int getSize();
}
