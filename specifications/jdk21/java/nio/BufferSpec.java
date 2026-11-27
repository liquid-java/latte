package latte.jdk21.java.nio;

import liquidjava.specification.*;
import specification.*;

/**
 * Result bounds follow the position/limit/capacity invariant. No exclusive ownership is promised
 * for shared backing arrays, slices or duplicates. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/Buffer.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/nio/Buffer.java
 */
@ExternalRefinementsFor("java.nio.Buffer")
public interface BufferSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int capacity();

  @Modifies({})
  @Refinement("_ >= 0")
  int position();

  @Modifies({})
  @Refinement("_ >= 0")
  int limit();

  @Modifies({})
  @Refinement("_ >= 0")
  int remaining();
}
