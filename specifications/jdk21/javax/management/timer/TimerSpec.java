package latte.jdk21.javax.management.timer;

import liquidjava.specification.*;
import specification.*;

/**
 * The notification count is a snapshot of the timer table size. Scheduled callbacks and externally
 * changing timer state are not modeled. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.management/javax/management/timer/Timer.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.management/share/classes/javax/management/timer/Timer.java
 */
@ExternalRefinementsFor("javax.management.timer.Timer")
public interface TimerSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getNbNotifications();
}
