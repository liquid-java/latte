package latte.jdk21.java.util.random;

import liquidjava.specification.*;
import specification.*;

/**
 * Provider implementations must honor the documented half-open ranges. No purity, seed, allocation
 * or distribution theorem is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/random/RandomGenerator.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/random/RandomGenerator.java
 */
@ExternalRefinementsFor("java.util.random.RandomGenerator")
public interface RandomGeneratorSpec {
  @Refinement("_ >= 0 && _ < bound")
  int nextInt(@Refinement("_ > 0") int bound);

  @Refinement("_ >= 0 && _ < bound")
  long nextLong(@Refinement("_ > 0") long bound);

  @Refinement("_ >= 0.0 && _ < 1.0")
  double nextDouble();

  @Refinement("_ >= 0.0 && _ < 1.0")
  float nextFloat();
}
