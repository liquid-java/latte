// Reduced independently from OpenJDK ArrayList/SubListModCount.java.
class LocalListView {
  static void createView() {
    var root = new java.util.ArrayList<Integer>();
    var view = root.subList(0, 0); // The view captures its backing list locally.
    root.add(42); // Local control is retained; this now verifies.
  }
}
