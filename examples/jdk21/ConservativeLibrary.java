/** Run with --jdk all. Conservative APIs are usable without assuming unprovided refinements. */
class ConservativeLibrary {
  int concurrentSnapshot(java.util.concurrent.atomic.AtomicInteger counter) {
    return counter.get();
  }

  long pipeline() {
    int[] visits = {0};
    var pipeline = java.util.stream.Stream.of(1, 2).map(value -> value + visits[0]++);
    // The callback may escape with its captures. No local ownership or value fact is retained.
    return pipeline.count();
  }

  java.util.List<Integer> view(java.util.List<Integer> values) {
    // An unmodifiable view can still alias values; the result remains shared.
    return java.util.Collections.unmodifiableList(values);
  }
}
