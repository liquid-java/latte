package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LargeProofTest {
  @Test
  void deepConjunctionPreservesEveryConstraintWithoutRecursiveTranslation() {
    var x = new Term.Symbol("x", Term.Sort.INT);
    Term assumptions = Term.TRUE;
    for (int i = 0; i < 10_000; i++)
      assumptions = Term.and(assumptions, Term.binary(">=", x, Term.integer(i)));
    try (var prover = new Prover(Map.of())) {
      assertTrue(prover.prove(assumptions, Term.binary(">=", x, Term.integer(9_999))).proven());
      assertFalse(prover.prove(assumptions, Term.binary(">=", x, Term.integer(10_000))).proven());
    }
  }

  @Test
  void deepDisjunctionPreservesEveryAlternativeWithoutRecursiveTranslation() {
    var x = new Term.Symbol("x", Term.Sort.INT);
    Term alternatives = Term.FALSE;
    for (int i = 0; i < 10_000; i++)
      alternatives = Term.or(Term.equal(x, Term.integer(i)), alternatives);
    try (var prover = new Prover(Map.of())) {
      assertTrue(prover.prove(Term.equal(x, Term.integer(0)), alternatives).proven());
      assertTrue(prover.prove(Term.equal(x, Term.integer(9_999)), alternatives).proven());
      assertFalse(prover.prove(Term.equal(x, Term.integer(10_000)), alternatives).proven());
    }
  }
}
