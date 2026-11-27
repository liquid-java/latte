import liquidjava.specification.Refinement;
import specification.Borrowed;

// An unannotated reference parameter is shared. JDK tests supply no ownership annotations.
class SharedArrayHelper {
  static byte[] unannotated(byte[] values) {
    // Reports PERMISSION and SHARED_REFINEMENT at this argument.
    return java.util.Arrays.copyOf(values, 1);
  }

  static byte[] annotated(@Borrowed @Refinement("_ != null") byte[] values) {
    return java.util.Arrays.copyOf(values, 1);
  }

  static byte[] local() {
    byte[] values = {1, 2};
    return java.util.Arrays.copyOf(values, 1);
  }
}
