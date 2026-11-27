package latte.jdk21.java.io;

import liquidjava.specification.*;
import specification.*;

/**
 * The constructor retains its byte array, so it is not Borrowed. close has no effect and reads
 * remain legal afterward. Byte results are unsigned or -1 for end of stream. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ByteArrayInputStream.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/io/ByteArrayInputStream.java
 */
@ExternalRefinementsFor("java.io.ByteArrayInputStream")
public interface ByteArrayInputStreamSpec {
  @Modifies({})
  void ByteArrayInputStream(byte[] buffer);

  @Modifies("this.*")
  @Refinement("_ >= -1 && _ <= 255")
  int read();

  @Modifies({})
  @Refinement("_")
  boolean markSupported();

  @Modifies({})
  @StateRefinement(from = "true")
  void close();

  @Modifies({"this.*", "destination[*]"})
  @Refinement("_ >= -1 && _ <= count")
  int read(
      @Borrowed @Refinement("_ != null") byte[] destination,
      @Refinement("_ >= 0 && count >= 0 && _ + count <= length(destination)") int offset,
      int count);
}
