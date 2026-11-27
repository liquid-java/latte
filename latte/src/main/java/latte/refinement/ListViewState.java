package latte.refinement;

import java.util.*;

/** Path-sensitive view relations live in the persistent symbolic store, never in a global cache. */
record ListViewState(Term backing, Term from, Term length, Term expectedVersion) {
  private static String key(Term view, String part) {
    return "view:" + part + ":" + view;
  }

  static Optional<ListViewState> on(Term view, TypingState state) {
    var values = state.values();
    var backing = values.get(key(view, "backing"));
    if (backing == null) return Optional.empty();
    return Optional.of(
        new ListViewState(
            backing,
            values.get(key(view, "from")),
            values.get(key(view, "length")),
            values.get(key(view, "expected"))));
  }

  static List<Term> identities(TypingState state) {
    return state.values().entrySet().stream()
        .filter(e -> e.getKey().startsWith("view:identity:"))
        .map(Map.Entry::getValue)
        .distinct()
        .toList();
  }

  TypingState store(Term view, TypingState state) {
    return state
        .bind(key(view, "identity"), view)
        .bind(key(view, "backing"), backing)
        .bind(key(view, "from"), from)
        .bind(key(view, "length"), length)
        .bind(key(view, "expected"), expectedVersion);
  }
}
