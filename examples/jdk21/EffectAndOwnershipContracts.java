import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import liquidjava.specification.Refinement;

// Run with --jdk all. Examples use bundled summaries, without application specification stubs.
class EffectAndOwnershipContracts {
  static Object escaped;

  static void copiedInputsStayLocal() {
    byte[] input = {7, 8};
    var output = new ByteArrayOutputStream();
    output.write(input, 0, 1);
    byte[] copy = Arrays.copyOfRange(input, 1, 4);
    @Refinement("_ == 3")
    int paddedLength = copy.length;
    Arrays.fill(copy, 0, 1, (byte) 0);
    output.write(input, 1, 1);
    @Refinement("_ == 7")
    byte preservedInput = input[0];
    @Refinement("_ == 2")
    int count = output.size();
  }

  static void localViewUpdatesBacking() {
    var list = new ArrayList<Integer>();
    list.add(1);
    var view = list.subList(0, 1);
    var alias = view;
    alias.add(2);
    @Refinement("_ == 2")
    int backingSize = list.size();
    view.subList(0, 1).clear();
    @Refinement("_ == 1")
    int viewSize = alias.size();
  }

  static void writesInvalidateAliases() {
    int[] values = {7};
    int[] alias = values;
    Arrays.fill(values, 0, 1, 3);
    @Refinement("_ == 7")
    int stale = alias[0]; // Expect: REFINEMENT
  }

  static void viewsRemainCaptures() {
    var list = new ArrayList<Integer>();
    escaped = list.subList(0, 0);
    list.size(); // Expect: SHARED_REFINEMENT
  }
}
