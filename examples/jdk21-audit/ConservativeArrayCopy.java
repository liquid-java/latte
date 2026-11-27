// Reduced independently from the second write in OpenJDK ByteArrayOutputStream/Write.java.
class ConservativeArrayCopy {
  static void copyThenWrite() {
    byte[] data = new byte[4];
    var output = new java.io.ByteArrayOutputStream();
    output.write(data, 0, 2);
    byte[] first = output.toByteArray();
    java.util.Arrays.copyOfRange(data, 0, 2); // A fresh copy; source storage is preserved.
    // The explicit effects and copy summary retain local control through the second write.
    output.write(data, 2, 2);
    byte[] second = output.toByteArray();
  }
}
