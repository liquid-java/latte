import liquidjava.specification.Refinement;
import specification.Borrowed;

/** Run with --jdk all. Each marked line is deliberately rejected. */
class RejectedContracts {
  void shared(java.util.ArrayList<Integer> list) {
    list.size(); // Expect: SHARED_REFINEMENT
  }

  void bounds() {
    var list = new java.util.ArrayList<Integer>();
    list.get(0); // Expect: REFINEMENT
  }

  void minimumInteger() {
    @Refinement("_ >= 0") int value = Math.abs(Integer.MIN_VALUE); // Expect: REFINEMENT
  }

  void retainedInput(@Borrowed byte[] bytes) {
    new java.io.ByteArrayInputStream(bytes); // Expect: PERMISSION
  }

  void cachedFactory() {
    @Refinement("_ != null") Integer value = Integer.valueOf(1); // Expect: SHARED_REFINEMENT
  }
}
