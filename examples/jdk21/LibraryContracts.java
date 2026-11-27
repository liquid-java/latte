import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import liquidjava.specification.Refinement;
import specification.Borrowed;

/** Run with --jdk all, or select java.lang,java.util,java.io,java.time,javax.crypto.spec. */
class LibraryContracts {
  void arithmetic() {
    @Refinement("_ == -2147483648") int minimum = Math.abs(Integer.MIN_VALUE);
    @Refinement("_ == 7") int sum = Math.addExact(3, 4);
    @Refinement("_") boolean nan = Double.isNaN(Double.NaN);
  }

  void aliases() {
    ArrayList<Integer> list = new ArrayList<>();
    ArrayList<Integer> alias = list;
    alias.add(42);
    @Refinement("_ == 1") int size = list.size();
    list.clear();
    @Refinement("_") boolean empty = alias.isEmpty();
  }

  void copies(@Borrowed @Refinement("_ != null") int[] input) {
    @Refinement("_ != null && length(_) == 3") int[] copy = Arrays.copyOf(input, 3);
    Arrays.fill(copy, 7);
    // The call may mutate the contents, while Latte keeps ownership of the array.
    @Refinement("_ != null") int[] alias = copy;
  }

  void output() throws java.io.IOException {
    var bytes = new ByteArrayOutputStream();
    bytes.write(1);
    bytes.close(); // Closing an in-memory output stream has no effect.
    bytes.write(2);
    @Refinement("_ == 2") int size = bytes.size();
    @Refinement("_ != null && length(_) == 2") byte[] copy = bytes.toByteArray();
  }

  void sharedValueObservers(java.time.LocalDate date) {
    // A scalar result bound does not require a persistent refinement on the shared date.
    @Refinement("_ >= 1 && _ <= 12") int month = date.getMonthValue();
  }

  void copiedCryptoInput(@Borrowed @Refinement("_ != null") byte[] iv) {
    var parameters = new javax.crypto.spec.GCMParameterSpec(128, iv);
    @Refinement("_ != null") byte[] copy = parameters.getIV();
  }
}
