package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

/**
 * Range guards describe the non-throwing domain using mathematical sums, so overflow cannot bypass
 * them. requireNonNull returns an existing alias and is not treated as a fresh allocation.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/Objects.java
 */
@ExternalRefinementsFor("java.util.Objects")
public interface ObjectsSpec {
  @Modifies({})
  @Refinement("_ == (value == null)")
  boolean isNull(@Borrowed Object value);

  @Modifies({})
  @Refinement("_ == (value != null)")
  boolean nonNull(@Borrowed Object value);

  @Modifies({})
  @Refinement("_ == index")
  int checkIndex(@Refinement("_ >= 0 && _ < size") int index, int size);

  @Modifies({})
  @Refinement("_ == from")
  int checkFromToIndex(@Refinement("_ >= 0 && _ <= to && to <= size") int from, int to, int size);

  @Modifies({})
  @Refinement("_ == from")
  int checkFromIndexSize(
      @Refinement("_ >= 0 && count >= 0 && _ + count <= size") int from, int count, int size);

  @Modifies({})
  @Refinement("_ == index")
  long checkIndex(@Refinement("_ >= 0 && _ < size") long index, long size);

  @Modifies({})
  @Refinement("_ == from")
  long checkFromToIndex(
      @Refinement("_ >= 0 && _ <= to && to <= size") long from, long to, long size);

  @Modifies({})
  @Refinement("_ == from")
  long checkFromIndexSize(
      @Refinement("_ >= 0 && count >= 0 && _ + count <= size") long from, long count, long size);
}
