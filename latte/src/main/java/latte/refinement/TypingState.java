package latte.refinement;

import java.util.*;

/**
 * Persistent symbolic state. SSA symbols keep earlier branch assumptions stable after assignments.
 */
public record TypingState(Map<String, Term> values, Map<String, Term> versions, Term assumptions) {
  public TypingState {
    values = Map.copyOf(values);
    versions = Map.copyOf(versions);
  }

  public static TypingState empty() {
    return new TypingState(Map.of(), Map.of(), Term.TRUE);
  }

  public TypingState bind(String key, Term value) {
    var next = new HashMap<>(values);
    next.put(key, value);
    return new TypingState(next, versions, assumptions);
  }

  public TypingState version(String key, Term value) {
    var next = new HashMap<>(versions);
    next.put(key, value);
    return new TypingState(values, next, assumptions);
  }

  public TypingState assume(Term predicate) {
    return new TypingState(values, versions, Term.and(assumptions, predicate));
  }

  public static TypingState join(Term guard, TypingState yes, TypingState no) {
    return new TypingState(
        merge(guard, yes.values, no.values),
        merge(guard, yes.versions, no.versions),
        Term.or(yes.assumptions, no.assumptions));
  }

  private static Map<String, Term> merge(Term guard, Map<String, Term> yes, Map<String, Term> no) {
    var result = new HashMap<String, Term>();
    // Array contents are immutable definitions indexed by SSA versions, not mutable bindings.
    yes.forEach(
        (key, value) -> {
          if (key.startsWith("elements:")) result.put(key, value);
        });
    no.forEach(
        (key, value) -> {
          if (key.startsWith("elements:")) result.put(key, value);
        });
    yes.forEach(
        (key, value) -> {
          if (no.containsKey(key))
            result.put(
                key,
                value.equals(no.get(key)) ? value : new Term.Choice(guard, value, no.get(key)));
        });
    return result;
  }
}
