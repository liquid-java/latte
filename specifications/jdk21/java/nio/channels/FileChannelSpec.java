package latte.jdk21.java.nio.channels;

import liquidjava.specification.*;
import specification.*;

/**
 * Normal results are nonnegative positions and byte counts. No cross-call stability is promised for
 * external files. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileChannel.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/nio/channels/FileChannel.java
 */
@ExternalRefinementsFor("java.nio.channels.FileChannel")
public interface FileChannelSpec {
  @Refinement("_ >= 0")
  long position();

  @Refinement("_ >= 0")
  long size();
}
