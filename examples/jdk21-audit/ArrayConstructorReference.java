// Reduced independently from OpenJDK Objects/CheckIndex.java.
// Regression: verifies with --jdk all after fixing synthetic constructor metadata.
class ArrayConstructorReference {
  static Object[][] copy(java.util.List<Object[]> rows) {
    return rows.toArray(Object[][]::new);
  }
}
