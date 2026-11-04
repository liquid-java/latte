package latte.refinement;

import java.math.BigInteger;
import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.reference.CtTypeReference;

/** Java integral arithmetic, including promotion, wraparound and signed division. */
final class JavaArithmetic {
  private JavaArithmetic() {}

  static Term bounds(Term value, CtTypeReference<?> type) {
    if (type == null) return Term.TRUE;
    String name = type.getQualifiedName();
    int bits =
        switch (name) {
          case "byte" -> 8;
          case "short", "char" -> 16;
          case "int" -> 32;
          case "long" -> 64;
          default -> 0;
        };
    if (bits == 0) return Term.TRUE;
    BigInteger lower =
        name.equals("char") ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(bits - 1).negate();
    BigInteger upper =
        name.equals("char")
            ? BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
            : BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE);
    return Term.and(
        Term.binary(">=", value, new Term.Literal(lower.toString(), Term.Sort.INT)),
        Term.binary("<=", value, new Term.Literal(upper.toString(), Term.Sort.INT)));
  }

  static Term wrap(Term value, CtTypeReference<?> type) {
    if (type == null) return value;
    String name = type.getQualifiedName();
    int bits =
        switch (name) {
          case "byte" -> 8;
          case "short", "char" -> 16;
          case "int" -> 32;
          case "long" -> 64;
          default -> 0;
        };
    if (bits == 0) return value;
    var modulus = new Term.Literal(BigInteger.ONE.shiftLeft(bits).toString(), Term.Sort.INT);
    if (name.equals("char")) return Term.binary("%", value, modulus);
    var half = new Term.Literal(BigInteger.ONE.shiftLeft(bits - 1).toString(), Term.Sort.INT);
    return Term.binary("-", Term.binary("%", Term.binary("+", value, half), modulus), half);
  }

  static Term convert(Term value, CtTypeReference<?> type) {
    if (type == null
        || !type.isPrimitive()
        || type.getSimpleName().equals("boolean")
        || type.getSimpleName().equals("void")) return value;
    return wrap(new Term.Call("cast:" + type.getSimpleName(), List.of(value)), type);
  }

  static Term cast(Term value, CtExpression<?> expression) {
    // Spoon stores casts in source order, outermost first.
    var casts = expression.getTypeCasts();
    for (int i = casts.size() - 1; i >= 0; i--) value = convert(value, casts.get(i));
    return value;
  }

  static Term binary(BinaryOperatorKind kind, Term left, Term right, CtTypeReference<?> type) {
    if (Set.of(
            BinaryOperatorKind.BITAND,
            BinaryOperatorKind.BITOR,
            BinaryOperatorKind.BITXOR,
            BinaryOperatorKind.SL,
            BinaryOperatorKind.SR,
            BinaryOperatorKind.USR)
        .contains(kind)) {
      if (type != null && type.getQualifiedName().equals("boolean"))
        return Term.binary(
            kind == BinaryOperatorKind.BITAND
                ? "&&"
                : kind == BinaryOperatorKind.BITOR ? "||" : "!=",
            left,
            right);
      return new Term.Call(
          "bit:"
              + kind.name()
              + ":"
              + (type != null && type.getQualifiedName().equals("long") ? 64 : 32),
          List.of(left, right));
    }
    String operator =
        switch (kind) {
          case PLUS -> "+";
          case MINUS -> "-";
          case MUL -> "*";
          case DIV -> "/";
          case MOD -> "%";
          case EQ -> "==";
          case NE -> "!=";
          case LT -> "<";
          case LE -> "<=";
          case GT -> ">";
          case GE -> ">=";
          case AND -> "&&";
          case OR -> "||";
          default -> throw new IllegalArgumentException("Unsupported Java binary operator " + kind);
        };
    if (type != null && Set.of("float", "double").contains(type.getQualifiedName()))
      return Term.binary(operator, convert(left, type), convert(right, type));
    if (kind == BinaryOperatorKind.DIV || kind == BinaryOperatorKind.MOD) {
      // Java integer division truncates towards zero; the solver's integer division rounds down.
      var zero = Term.integer(0);
      var absLeft = new Term.Choice(Term.binary("<", left, zero), new Term.Unary("-", left), left);
      var absRight =
          new Term.Choice(Term.binary("<", right, zero), new Term.Unary("-", right), right);
      var magnitude = Term.binary("/", absLeft, absRight);
      var quotient =
          new Term.Choice(
              Term.binary("!=", Term.binary("<", left, zero), Term.binary("<", right, zero)),
              new Term.Unary("-", magnitude),
              magnitude);
      return wrap(
          kind == BinaryOperatorKind.DIV
              ? quotient
              : Term.binary("-", left, Term.binary("*", quotient, right)),
          type);
    }
    var result = Term.binary(operator, left, right);
    return Set.of(BinaryOperatorKind.PLUS, BinaryOperatorKind.MINUS, BinaryOperatorKind.MUL)
            .contains(kind)
        ? wrap(result, type)
        : result;
  }
}
