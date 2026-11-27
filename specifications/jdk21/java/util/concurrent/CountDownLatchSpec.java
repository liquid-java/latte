package latte.jdk21.java.util.concurrent;

import liquidjava.specification.*;
import specification.*;

/**
 * The count is initialized nonnegative and decremented only while positive. The observation is a
 * snapshot, with no persistent receiver state. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/concurrent/CountDownLatch.java
 */
@ExternalRefinementsFor("java.util.concurrent.CountDownLatch")
public interface CountDownLatchSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  long getCount();
}
