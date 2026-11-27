package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

/**
 * Elements can be retained, so input and result elements remain shared. Ghost state models element
 * count, not capacity or element identity. Callback-driven operations remain conservative.
 * ArrayList subList views retain their backing relationship through ReturnsView. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/ArrayList.java
 */
@ExternalRefinementsFor("java.util.ArrayList")
@Ghost("int listSize")
public interface ArrayListSpec<E> {
  @Modifies({})
  @StateRefinement(to = "listSize(this) == 0")
  void ArrayList();

  @Modifies({})
  @StateRefinement(to = "listSize(this) == 0")
  void ArrayList(@Refinement("_ >= 0") int initialCapacity);

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == listSize(this) && _ >= 0")
  int size();

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == (listSize(this) == 0)")
  boolean isEmpty();

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) + 1")
  @Refinement("_")
  boolean add(E value);

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) + 1")
  void add(@Refinement("_ >= 0 && _ <= listSize(this)") int index, E value);

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == 0")
  void clear();

  @Modifies({})
  @StateRefinement(from = "true")
  E get(@Refinement("_ >= 0 && _ < listSize(this)") int index);

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) - 1")
  E remove(@Refinement("_ >= 0 && _ < listSize(this)") int index);

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) + 1")
  void addFirst(E value);

  @Modifies("this.*")
  @StateRefinement(to = "listSize(this) == listSize(old(this)) + 1")
  void addLast(E value);

  @Modifies({})
  @StateRefinement(from = "listSize(this) > 0")
  E getFirst();

  @Modifies({})
  @StateRefinement(from = "listSize(this) > 0")
  E getLast();

  @Modifies("this.*")
  @StateRefinement(from = "listSize(this) > 0", to = "listSize(this) == listSize(old(this)) - 1")
  E removeFirst();

  @Modifies("this.*")
  @StateRefinement(from = "listSize(this) > 0", to = "listSize(this) == listSize(old(this)) - 1")
  E removeLast();

  @Modifies({})
  @ReturnsView(of = "this", from = "fromIndex", toExclusive = "toIndex")
  java.util.List<E> subList(
      @Refinement("_ >= 0 && _ <= listSize(this)") int fromIndex,
      @Refinement("_ >= fromIndex && _ <= listSize(this)") int toIndex);
}
