// Reduced independently from test/jdk/java/lang/Math/Clamp.java in OpenJDK 21.
// Run with --jdk all. Literal bounds verify; loop-carried table relations do not.
class ClampTable {
  static void literalBounds() {
    long result = Math.clamp(0L, 1L, 2L);
  }

  static void validTable() {
    long[][] rows = {{0, 1, 2}, {3, 0, 2}};
    for (long[] row : rows) {
      long result = Math.clamp(row[0], row[1], row[2]); // Expect: REFINEMENT
    }
  }

  static void expectedException() {
    try {
      Math.clamp(0L, 2L, 1L); // Expect: REFINEMENT
      throw new AssertionError("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // Catching the exception does not discharge the call's refinement precondition.
    }
  }
}
