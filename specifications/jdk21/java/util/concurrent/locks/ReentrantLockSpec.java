package latte.jdk21.java.util.concurrent.locks;

import liquidjava.specification.*;
import specification.*;

/**
 * Hold count is for the current thread and queue length is an estimate; no stable concurrency
 * invariant is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/concurrent/locks/ReentrantLock.java
 */
@ExternalRefinementsFor("java.util.concurrent.locks.ReentrantLock")
public interface ReentrantLockSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getHoldCount();

  @Modifies({})
  @Refinement("_ >= 0")
  int getQueueLength();
}
