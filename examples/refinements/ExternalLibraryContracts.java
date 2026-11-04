import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList list)")
interface ArrayListContracts<E> {
  @StateRefinement(to = "size(this) == 0")
  void ArrayList();

  @StateRefinement(to = "size(this) == size(old(this)) + 1")
  boolean add(E element);

  @StateRefinement(from = "true")
  @Refinement("_ == size(this)")
  int size();

  @StateRefinement(from = "size(this) > 0", to = "size(this) == size(old(this)) - 1")
  E remove(@Refinement("_ >= 0 && _ < size(this)") int index);
}

@ExternalRefinementsFor("java.util.Arrays")
interface ArrayContracts {
  void fill(@Borrowed int[] values, int value);
}

@ExternalRefinementsFor("java.util.Objects")
interface ObjectContracts {
  @Free
  @Refinement("_ != null")
  Object requireNonNull(@Free Object value);
}

class ExternalLibraryContracts {
  void aliasesShareLibraryState() {
    java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
    java.util.ArrayList<Integer> alias = list;
    alias.add(42);
    @Refinement("_ == 1") int size = list.size();
    list.remove(1); // Expect: REFINEMENT
  }

  void borrowedArrayRemainsLocal() {
    int[] values = {0};
    java.util.Arrays.fill(values, 3);
    @Refinement("_ != null") int[] alias = values;
    @Refinement("_ == 0") int stale = alias[0]; // Expect: REFINEMENT
  }

  void ownershipTransfer() {
    Object value = new Object();
    Object oldAlias = value;
    @Refinement("_ != null") Object result = java.util.Objects.requireNonNull(value);
    Object consumed = oldAlias; // Expect: PERMISSION
  }

  void sharedReceiver(java.util.ArrayList<Integer> list) {
    list.size(); // Expect: SHARED_REFINEMENT
  }
}
