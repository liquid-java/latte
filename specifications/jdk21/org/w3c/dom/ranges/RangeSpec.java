package latte.jdk21.org.w3c.dom.ranges;

import liquidjava.specification.*;
import specification.*;

/**
 * Offsets are nonnegative node or character positions. Mutation can relocate a live range; no
 * stable relation to its container is asserted. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/w3c/dom/ranges/Range.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.xml/share/classes/org/w3c/dom/ranges/Range.java
 */
@ExternalRefinementsFor("org.w3c.dom.ranges.Range")
public interface RangeSpec {
  @Refinement("_ >= 0")
  int getStartOffset();

  @Refinement("_ >= 0")
  int getEndOffset();
}
