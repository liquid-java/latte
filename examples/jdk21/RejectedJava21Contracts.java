import liquidjava.specification.Refinement;
import specification.Borrowed;

/** Run with --jdk all on Java 21. Every marked line is deliberately rejected. */
class RejectedJava21Contracts {
  void emptyEnd() {
    var values = new java.util.ArrayList<Integer>();
    values.getLast(); // Expect: STATE
  }

  void invalidClamp() {
    Math.clamp(2L, 10, 0); // Expect: REFINEMENT
  }

  void reversedView(java.util.List<Integer> values) {
    @Refinement("_ != null") java.util.List<Integer> alias = values.reversed(); // Expect: SHARED_REFINEMENT
  }

  void virtualCapture(@Borrowed int[] values) {
    Thread.startVirtualThread(() -> values[0]++); // Expect: PERMISSION
  }

  void threadCounter(Thread thread) {
    @Refinement("_ > 0") long id = thread.threadId(); // Expect: REFINEMENT
  }

  void mapCounter(java.util.Map<?, ?> values) {
    @Refinement("_ >= 0") int size = values.size(); // Expect: REFINEMENT
  }
}
