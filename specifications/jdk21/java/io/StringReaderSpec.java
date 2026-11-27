package latte.jdk21.java.io;

import liquidjava.specification.*;
import specification.*;

/**
 * Normal reads return a UTF-16 code unit or -1. The constructor retains the String; close
 * invalidates access. skip is omitted because negative skipping is supported. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/StringReader.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/io/StringReader.java
 */
@ExternalRefinementsFor("java.io.StringReader")
public interface StringReaderSpec {
  @Modifies({})
  void StringReader(String source);

  @Modifies("this.*")
  @Refinement("_ >= -1 && _ <= 65535")
  int read();

  @Modifies("this.*")
  void close();

  @Modifies({"this.*", "destination[*]"})
  @Refinement("_ >= -1 && _ <= count")
  int read(
      @Borrowed @Refinement("_ != null") char[] destination,
      @Refinement("_ >= 0 && count >= 0 && _ + count <= length(destination)") int offset,
      int count);
}
