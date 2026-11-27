import java.util.ArrayList;
import java.util.List;
import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.ArrayList")
@Ghost("int listSize")
interface ViewListContracts<E> {
  @Modifies({})
  @StateRefinement(to = "listSize(this) == 0")
  void ArrayList();

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) + 1")
  boolean add(E value);

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == listSize(this)")
  int size();

  @Modifies({})
  @ReturnsView(of = "this", from = "fromIndex", toExclusive = "toIndex")
  List<E> subList(int fromIndex, int toIndex);
}

class ExternalListViews {
  static Object escaped;

  static void localAliases() {
    ArrayList<Integer> list = new ArrayList<>();
    list.add(1);
    List<Integer> view = list.subList(0, 1);
    List<Integer> alias = view;
    alias.add(2);
    @Refinement("_ == 2") int throughView = view.size();
    @Refinement("_ == 2") int throughBacking = list.size();
    view.subList(0, 1).clear();
    @Refinement("_ == 1") int afterNestedView = list.size();
  }

  static void staleView() {
    ArrayList<Integer> list = new ArrayList<>();
    List<Integer> view = list.subList(0, 0);
    list.add(1);
    view.size(); // Expect: STATE
  }

  static void escapingView() {
    ArrayList<Integer> list = new ArrayList<>();
    List<Integer> view = list.subList(0, 0);
    escaped = view;
    list.size(); // Expect: SHARED_REFINEMENT
  }
}
