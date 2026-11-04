package latte.refinement;

import java.math.BigDecimal;
import java.util.*;

/** Immutable refinement syntax, independent of Spoon and the solver. */
public sealed interface Term
    permits Term.Literal,
        Term.Name,
        Term.Symbol,
        Term.Unary,
        Term.Binary,
        Term.Choice,
        Term.Call,
        Term.ArraySymbol,
        Term.ArrayConstant,
        Term.ArrayStore,
        Term.ArraySelect {
  enum Sort {
    BOOL,
    INT,
    REAL,
    FLOAT32,
    FLOAT64,
    REF
  }

  record Literal(String value, Sort sort) implements Term {}

  record Name(String value) implements Term {}

  record Symbol(String name, Sort sort) implements Term {}

  record Unary(String operator, Term operand) implements Term {}

  record Binary(String operator, Term left, Term right) implements Term {}

  record Choice(Term condition, Term yes, Term no) implements Term {}

  record Call(String name, List<Term> arguments) implements Term {
    public Call {
      arguments = List.copyOf(arguments);
    }
  }

  record ArraySymbol(String name, Sort elementSort) implements Term {}

  record ArrayConstant(Term value) implements Term {}

  record ArrayStore(Term array, Term index, Term value) implements Term {}

  record ArraySelect(Term array, Term index) implements Term {}

  Term TRUE = new Literal("true", Sort.BOOL);
  Term FALSE = new Literal("false", Sort.BOOL);
  Term NULL = new Literal("null", Sort.REF);

  static Term integer(long value) {
    return new Literal(Long.toString(value), Sort.INT);
  }

  static Term number(String text) {
    String value = text.replace("_", "").replaceAll("[fFdDlL]$", "");
    if (value.startsWith("0x") || value.startsWith("0X"))
      return new Literal(new java.math.BigInteger(value.substring(2), 16).toString(), Sort.INT);
    return new Literal(
        new BigDecimal(value).toPlainString(),
        value.contains(".") || value.contains("e") || value.contains("E") ? Sort.REAL : Sort.INT);
  }

  static Term binary(String operator, Term left, Term right) {
    return new Binary(operator, left, right);
  }

  static Term not(Term value) {
    return new Unary("!", value);
  }

  static Term and(Term left, Term right) {
    if (left.equals(TRUE)) return right;
    if (right.equals(TRUE)) return left;
    return new Binary("&&", left, right);
  }

  static Term or(Term left, Term right) {
    return new Binary("||", left, right);
  }

  static Term equal(Term left, Term right) {
    return new Binary("==", left, right);
  }

  default Term substitute(Map<String, Term> bindings) {
    if (this instanceof Name name) return bindings.getOrDefault(name.value(), this);
    if (this instanceof Unary unary)
      return new Unary(unary.operator(), unary.operand().substitute(bindings));
    if (this instanceof Binary binary)
      return new Binary(
          binary.operator(),
          binary.left().substitute(bindings),
          binary.right().substitute(bindings));
    if (this instanceof Choice choice)
      return new Choice(
          choice.condition().substitute(bindings),
          choice.yes().substitute(bindings),
          choice.no().substitute(bindings));
    if (this instanceof Call call)
      return new Call(
          call.name(), call.arguments().stream().map(a -> a.substitute(bindings)).toList());
    return this;
  }
}
