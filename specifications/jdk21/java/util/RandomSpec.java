package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

/**
 * Documented half-open output ranges. Generator calls mutate state; nextBytes writes but does not
 * retain its primitive array. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/Random.java
 */
@ExternalRefinementsFor("java.util.Random")
public interface RandomSpec {
  @Refinement("_ >= 0 && _ < bound")
  int nextInt(@Refinement("_ > 0") int bound);

  @Refinement("_ >= 0.0 && _ < 1.0")
  double nextDouble();

  @Refinement("_ >= 0.0 && _ < 1.0")
  float nextFloat();

  void nextBytes(@Borrowed @Refinement("_ != null") byte[] bytes);
}
