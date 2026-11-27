package latte.jdk21.java.nio.file;

import liquidjava.specification.*;
import specification.*;

/**
 * Name count includes zero for roots and the documented empty-path behavior. Provider-produced
 * paths and views have no freshness assertion. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Path.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/nio/file/Path.java
 */
@ExternalRefinementsFor("java.nio.file.Path")
public interface PathSpec {
  @Refinement("_ >= 0")
  int getNameCount();
}
