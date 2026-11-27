package latte.jdk21.java.net;

import liquidjava.specification.*;
import specification.*;

/**
 * Absent ports use -1. URI syntax permits port values above 65535, so the socket-port upper bound
 * does not apply. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/net/URI.java
 */
@ExternalRefinementsFor("java.net.URI")
public interface URISpec {
  @Modifies({})
  @Refinement("_ >= -1")
  int getPort();
}
