package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Character-array construction copies its input. Length counts UTF-16 code units. toCharArray
 * returns a fresh array; substring, intern and cached factories have no freshness claim.
 * Documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/String.java
 */
@ExternalRefinementsFor("java.lang.String")
@Ghost("int stringLength")
public interface StringSpec {
  @Modifies({})
  @StateRefinement(to = "stringLength(this) == 0")
  void String();

  @Modifies({})
  @StateRefinement(to = "stringLength(this) == length(value)")
  void String(@Borrowed @Refinement("_ != null") char[] value);

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == stringLength(this) && _ >= 0")
  int length();

  @Modifies({})
  @StateRefinement(from = "true")
  @Refinement("_ == (stringLength(this) == 0)")
  boolean isEmpty();

  @Modifies({})
  @StateRefinement(from = "true")
  char charAt(@Refinement("_ >= 0 && _ < stringLength(this)") int index);

  @Modifies({})
  @StateRefinement(from = "true")
  @Free
  @Refinement("_ != null && length(_) == stringLength(this)")
  char[] toCharArray();

  @Modifies("destination[*]")
  @StateRefinement(from = "true")
  void getChars(
      @Refinement("_ >= 0 && _ <= end && end <= stringLength(this)") int begin,
      int end,
      @Borrowed @Refinement("_ != null") char[] destination,
      @Refinement("_ >= 0 && _ + end - begin <= length(destination)") int offset);
}
