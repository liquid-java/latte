package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Unicode code-point ranges, UTF-16 width and numeric-value sentinel bounds; Unicode category
 * tables are not approximated. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Character.java
 */
@ExternalRefinementsFor("java.lang.Character")
public interface CharacterSpec {
  @Modifies({})
  @Refinement("_ == (codePoint >= 0 && codePoint <= 1114111)")
  boolean isValidCodePoint(int codePoint);

  @Modifies({})
  @Refinement("_ == (codePoint >= 0 && codePoint <= 65535)")
  boolean isBmpCodePoint(int codePoint);

  @Modifies({})
  @Refinement("_ == (codePoint >= 65536 && codePoint <= 1114111)")
  boolean isSupplementaryCodePoint(int codePoint);

  @Modifies({})
  @Refinement("_ == (codePoint >= 65536 ? 2 : 1)")
  int charCount(int codePoint);

  @Modifies({})
  @Refinement("_ == -1 || (_ >= 0 && _ < radix && radix >= 2 && radix <= 36)")
  int digit(char value, int radix);

  @Modifies({})
  @Refinement("_ >= -2")
  int getNumericValue(char value);

  @Modifies({})
  @Refinement("_ == -1 || (_ >= 0 && _ < radix && radix >= 2 && radix <= 36)")
  int digit(int value, int radix);

  @Modifies({})
  @Refinement("_ >= -2")
  int getNumericValue(int value);

  @Modifies({})
  @Refinement("_ == high - low")
  int compare(char high, char low);
}
