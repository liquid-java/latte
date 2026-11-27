import liquidjava.specification.Refinement;
import specification.Borrowed;

// Investigation reproducer: Java array lengths always lie in [0, Integer.MAX_VALUE].
// Run with --jdk all. The current checker omits these bounds for incoming arrays.
class ArrayLengthBounds {
  @Refinement("_ >= 0")
  static int parameter(@Borrowed int[] data) {
    return data.length; // Expect: REFINEMENT
  }

  @Refinement("_ >= 0")
  static int local() {
    int[] data = new int[3];
    return data.length;
  }
}
