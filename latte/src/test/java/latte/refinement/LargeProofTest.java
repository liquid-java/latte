package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LargeProofTest {
  @Test
  void deepAlternatingBranchesPreserveSharedConditions() {
    var x = new Term.Symbol("x", Term.Sort.INT);
    Term paths = Term.equal(x, Term.integer(7));
    for (int i = 0; i < 10_000; i++) {
      var branch = new Term.Symbol("branch" + i, Term.Sort.BOOL);
      paths = Term.or(Term.and(branch, paths), Term.and(Term.not(branch), paths));
    }
    try (var prover = new Prover(Map.of())) {
      prover.validate(paths);
      assertTrue(prover.satisfiable(paths).proven());
      assertTrue(prover.prove(paths, Term.equal(x, Term.integer(7))).proven());
      var rejected = prover.prove(paths, Term.equal(x, Term.integer(8)));
      assertFalse(rejected.proven());
      assertFalse(rejected.detail().contains("Solver could not decide"), rejected.detail());
    }
  }

  @Test
  void deepArrayStoresChoicesAndCallsTranslateWithoutRecursion() {
    Term contents = new Term.ArrayConstant(Term.integer(0));
    for (int i = 0; i < 10_000; i++)
      contents = new Term.ArrayStore(contents, Term.integer(0), Term.integer(i));
    Term value = new Term.ArraySelect(contents, Term.integer(0));
    for (int i = 0; i < 10_000; i++)
      value =
          new Term.Choice(
              Term.TRUE,
              new Term.Call("cast:int", List.of(new Term.Unary("-", value))),
              Term.integer(-1));
    try (var prover = new Prover(Map.of())) {
      assertTrue(prover.prove(Term.TRUE, Term.equal(value, Term.integer(9_999))).proven());
      var rejected = prover.prove(Term.TRUE, Term.equal(value, Term.integer(-1)));
      assertFalse(rejected.proven());
      assertFalse(rejected.detail().contains("Solver could not decide"), rejected.detail());
    }
  }

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
