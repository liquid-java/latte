import java.util.Arrays;
import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.Arrays")
interface EffectArrayContracts {
  @Modifies("array[*]")
  void fill(@Borrowed int[] array, int value);

  @Modifies({})
  @Free
  int[] copyOf(@Borrowed int[] array, int newLength);
}

class ExternalEffectSummaries {
  static void copyPreservesInput() {
    int[] input = {7};
    int[] copy = Arrays.copyOf(input, 1);
    copy[0] = 4;
    @Refinement("_ == 7") int unchanged = input[0];
  }

  static void writesInvalidateLocalAliases() {
    int[] input = {7};
    int[] alias = input;
    int[] other = {9};
    Arrays.fill(input, 0);
    @Refinement("_ == 9") int unaffected = other[0];
    @Refinement("_ == 7") int stale = alias[0]; // Expect: REFINEMENT
  }
}
