package latte.jdk21.java.io;

import liquidjava.specification.*;
import specification.*;

/**
 * Writes copy bytes; toByteArray copies the buffer; reset clears the count. Closing this in-memory
 * stream does not disable later writes. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ByteArrayOutputStream.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/io/ByteArrayOutputStream.java
 */
@ExternalRefinementsFor("java.io.ByteArrayOutputStream")
@Ghost("int byteCount")
public interface ByteArrayOutputStreamSpec {
  @Modifies({})
  @StateRefinement(to = "byteCount(this) == 0")
  void ByteArrayOutputStream();

  @Modifies({})
  @StateRefinement(to = "byteCount(this) == 0")
  void ByteArrayOutputStream(@Refinement("_ >= 0") int capacity);

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == byteCount(this) && _ >= 0")
  int size();

  @Modifies("this.*")
  @StateRefinement(to = "byteCount(this) == byteCount(old(this)) + 1")
  void write(int value);

  @Modifies("this.*")
  @StateRefinement(to = "byteCount(this) == byteCount(old(this)) + count")
  void write(
      @Borrowed @Refinement("_ != null") byte[] bytes,
      @Refinement("_ >= 0 && count >= 0 && _ + count <= length(bytes)") int offset,
      int count);

  @Modifies("this.*")
  @StateRefinement(to = "byteCount(this) == 0")
  void reset();

  @Modifies({})
  @StateRefinement(from = "true")
  @Free
  @Refinement("_ != null && length(_) == byteCount(this)")
  byte[] toByteArray();

  @Modifies({})
  @StateRefinement(from = "true")
  void close();
}
