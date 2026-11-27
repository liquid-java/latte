package latte.jdk21.java.util.regex;

import liquidjava.specification.*;
import specification.*;

/**
 * Unmatched optional groups use -1. Calls requiring a previous successful match may throw; no fresh
 * matcher or retained CharSequence ownership is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/MatchResult.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/regex/MatchResult.java
 */
@ExternalRefinementsFor("java.util.regex.MatchResult")
public interface MatchResultSpec {
  @Refinement("_ >= 0")
  int start();

  @Refinement("_ >= 0")
  int end();

  @Refinement("_ >= -1")
  int start(int group);

  @Refinement("_ >= -1")
  int end(int group);

  @Refinement("_ >= 0")
  int groupCount();
}
