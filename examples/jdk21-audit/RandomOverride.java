// Investigation reproducer: a test double can intentionally violate an API contract.
// Run with --jdk all. RandomGenerator.nextDouble() must return a value in [0, 1).
class RandomOverride implements java.util.random.RandomGenerator {
  public long nextLong() {
    return 0;
  }

  public double nextDouble() {
    return Double.MAX_VALUE; // Expect: REFINEMENT
  }
}
