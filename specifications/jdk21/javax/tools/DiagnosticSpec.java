package latte.jdk21.javax.tools;

import liquidjava.specification.*;
import specification.*;

/**
 * Positions are zero-based; line and column numbers are one-based. NOPOS is -1. Tool providers must
 * honor the Diagnostic interface contract. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/Diagnostic.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.compiler/share/classes/javax/tools/Diagnostic.java
 */
@ExternalRefinementsFor("javax.tools.Diagnostic")
public interface DiagnosticSpec {
  @Refinement("_ >= -1")
  long getPosition();

  @Refinement("_ >= -1")
  long getStartPosition();

  @Refinement("_ >= -1")
  long getEndPosition();

  @Refinement("_ == -1 || _ >= 1")
  long getLineNumber();

  @Refinement("_ == -1 || _ >= 1")
  long getColumnNumber();
}
