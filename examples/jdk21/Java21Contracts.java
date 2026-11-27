import liquidjava.specification.Refinement;

/** Run with --jdk all on Java 21. */
class Java21Contracts {
  void arithmetic() {
    @Refinement("_ == 100") int saturated = Math.clamp(Long.MAX_VALUE, 0, 100);
    @Refinement("_ == -3") int floor = Math.floorDivExact(-7, 3);
    @Refinement("_ == -2") long ceiling = StrictMath.ceilDivExact(-7L, 3L);
  }

  void sequencedEnds() {
    var values = new java.util.ArrayList<Integer>();
    var alias = values;
    alias.addFirst(1);
    values.addLast(2);
    @Refinement("_ == 2") int size = alias.size();
    // Retrieved elements remain shared; the contract promises no fresh element or exact alias.
    values.getFirst();
  }

  void sharedObservations(java.lang.annotation.RetentionPolicy retention) {
    @Refinement("_ >= 0 && _ < 3") int index = retention.ordinal();
  }
}
