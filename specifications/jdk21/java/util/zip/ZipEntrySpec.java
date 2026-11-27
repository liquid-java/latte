package latte.jdk21.java.util.zip;

import liquidjava.specification.*;
import specification.*;

/**
 * Only the unsigned CRC and unknown sentinel are constrained. Compressed size accepts arbitrary
 * signed setter values; ZIP64 sizes and unsupported compression methods are not restricted.
 * Pre-epoch timestamps are legal. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/zip/ZipEntry.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/zip/ZipEntry.java
 */
@ExternalRefinementsFor("java.util.zip.ZipEntry")
public interface ZipEntrySpec {
  @Modifies({})
  @Refinement("_ >= -1 && _ <= 4294967295")
  long getCrc();
}
