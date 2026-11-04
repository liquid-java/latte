package examples;

public class MyStackTest {

  public static void main(String[] args) {

    // Define and initialize queues
    LinkedList<Integer> qev1;
    qev1 = new LinkedList<>();
    qev1.add(100);

    // Get an iterator for the queue
    Iterator<Integer> iterator = qev1.iterator();

    try {
      iterator.remove(); // Error no call to next before remove
    } catch (UnsupportedOperationException e) {
      System.out.println("Calling Iterator.remove() and throwing exception.");
    }
  }
}

// @ExternalRefinementsFor("java.util.Iterator")
// @StateSet({"start", "ready", "inNext"})
class Iterator<N> {

  // @StateRefinement(to = "start(this)")
  public Iterator() {}

  // @StateRefinement(to = "ready(this)")
  public boolean hasNext() {
    return true;
  }

  // @StateRefinement(from = "ready(this)", to = "inNext(this)")
  public Object next() {
    return null;
  }

  // @StateRefinement(from = "inNext(this)", to = "start(this)")
  public void remove() {}
}

// @ExternalRefinementsFor("java.util.LinkedList")
class LinkedList<T> {

  public Iterator<T> iterator() {
    return null;
  }

  public void add(T t) {}
}
