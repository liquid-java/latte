package latte.jdk21.java.util.zip;

import liquidjava.specification.*;
import specification.*;

/**
 * Checksum is an unsigned 32-bit value in a long. Primitive byte-array inputs are not retained; the
 * checksum changes on update/reset. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/zip/CRC32C.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/zip/CRC32C.java
 */
@ExternalRefinementsFor("java.util.zip.CRC32C")
public interface CRC32CSpec {
  @Modifies({})
  @Refinement("_ >= 0 && _ <= 4294967295")
  long getValue();

  @Modifies("this.*")
  void reset();

  @Modifies("this.*")
  void update(int value);

  @Modifies("this.*")
  void update(
      @Borrowed @Refinement("_ != null") byte[] bytes,
      @Refinement("_ >= 0 && count >= 0 && _ + count <= length(bytes)") int offset,
      int count);

  @Modifies({})
  void CRC32C();
}
