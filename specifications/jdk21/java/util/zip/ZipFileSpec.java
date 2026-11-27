package latte.jdk21.java.util.zip;

import liquidjava.specification.*;
import specification.*;

/**
 * Number of central-directory entries on normal return; accessing a closed ZIP file can throw.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/zip/ZipFile.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/zip/ZipFile.java
 */
@ExternalRefinementsFor("java.util.zip.ZipFile")
public interface ZipFileSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int size();
}
