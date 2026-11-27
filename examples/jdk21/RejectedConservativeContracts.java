import liquidjava.specification.Refinement;
import specification.Borrowed;

/** Run with --jdk all. Every marked assumption needs a stronger, justified contract. */
class RejectedConservativeContracts {
  void callback(@Borrowed int[] values) {
    java.util.stream.Stream.of(1).map(value -> values[0]++); // Expect: PERMISSION
  }

  void view(java.util.List<Integer> values) {
    @Refinement("_ != null") java.util.List<Integer> alias = java.util.Collections.unmodifiableList(values); // Expect: SHARED_REFINEMENT
  }

  void observation(java.util.concurrent.atomic.AtomicInteger counter) {
    @Refinement("_ == 0") int value = counter.get(); // Expect: REFINEMENT
  }

  void constructor() {
    @Refinement("_ != null") java.util.Timer timer = new java.util.Timer(); // Expect: SHARED_REFINEMENT
  }
}
