import java.util.Random;
import liquidjava.specification.Refinement;

// Investigation reproducer: helper bodies are checked for every permitted input.
// Run with --jdk all. A valid caller alone does not refine the helper's parameter.
class HelperBounds {
  static int unchecked(int bound) {
    return new Random().nextInt(bound); // Expect: REFINEMENT
  }

  static int guarded(int bound) {
    if (bound <= 0) return 0;
    return new Random().nextInt(bound);
  }

  static int annotated(@Refinement("_ > 0") int bound) {
    return new Random().nextInt(bound);
  }

  static int caller() {
    return unchecked(10);
  }
}
