package latte.jdk21.java.util.concurrent;

import liquidjava.specification.*;
import specification.*;

/**
 * Queue length is a nonnegative estimate. availablePermits is deliberately omitted: initial permits
 * may be negative. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Semaphore.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/concurrent/Semaphore.java
 */
@ExternalRefinementsFor("java.util.concurrent.Semaphore")
public interface SemaphoreSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getQueueLength();
}
