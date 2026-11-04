package latte.api;

import spoon.reflect.declaration.CtElement;

/** A source-located failure. Unsupported constructs are failures, never successful proofs. */
public record Diagnostic(
    Kind kind, String message, String detail, String file, int line, int column) {
  public enum Kind {
    PERMISSION,
    SHARED_REFINEMENT,
    REFINEMENT,
    STATE,
    SYNTAX,
    ARGUMENT,
    UNSUPPORTED
  }

  public static Diagnostic at(Kind kind, String message, CtElement element) {
    return at(kind, message, "", element);
  }

  public static Diagnostic at(Kind kind, String message, String detail, CtElement element) {
    var position = element.getPosition();
    return position.isValidPosition()
        ? new Diagnostic(
            kind,
            message,
            detail,
            position.getFile().toString(),
            position.getLine(),
            position.getCompilationUnit().getOriginalSourceCode() == null
                ? 0
                : position.getColumn())
        : new Diagnostic(kind, message, detail, "<implicit>", 0, 0);
  }
}
