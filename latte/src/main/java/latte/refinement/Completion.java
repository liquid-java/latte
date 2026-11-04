package latte.refinement;

import spoon.reflect.declaration.CtElement;

/** An immutable control-flow outcome; finally may replace the outcome but must always run. */
record Completion(
    Kind kind, TypingState state, Term value, String label, String exceptionType, CtElement site) {
  enum Kind {
    NORMAL,
    RETURN,
    THROW,
    BREAK,
    CONTINUE,
    YIELD
  }

  static Completion normal(TypingState state) {
    return new Completion(Kind.NORMAL, state, Term.TRUE, null, null, null);
  }

  Completion withState(TypingState after) {
    return new Completion(kind, after, value, label, exceptionType, site);
  }

  boolean normal() {
    return kind == Kind.NORMAL;
  }
}
