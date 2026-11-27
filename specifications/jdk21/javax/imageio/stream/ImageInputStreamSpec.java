package latte.jdk21.javax.imageio.stream;

import liquidjava.specification.*;
import specification.*;

/**
 * Unsigned reads and bit offset bounds apply on normal completion. Reads may advance the position
 * and throw at EOF; no backing-stream ownership is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/stream/ImageInputStream.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/javax/imageio/stream/ImageInputStream.java
 */
@ExternalRefinementsFor("javax.imageio.stream.ImageInputStream")
public interface ImageInputStreamSpec {
  @Refinement("_ >= 0 && _ <= 7")
  int getBitOffset();

  @Refinement("_ >= 0")
  long getStreamPosition();

  @Refinement("_ >= 0 && _ <= 255")
  int readUnsignedByte();

  @Refinement("_ >= 0 && _ <= 65535")
  int readUnsignedShort();
}
