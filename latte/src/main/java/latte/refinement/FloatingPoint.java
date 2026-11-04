package latte.refinement;

import com.microsoft.z3.*;
import java.math.BigInteger;

/** IEEE 754 Java arithmetic. Conversion to reals is guarded against non-finite values. */
final class FloatingPoint {
  private final Context context;

  FloatingPoint(Context context) {
    this.context = context;
  }

  FPSort sort(Term.Sort sort) {
    return sort == Term.Sort.FLOAT32 ? context.mkFPSort32() : context.mkFPSort64();
  }

  FPExpr literal(String value, Term.Sort sort) {
    return sort == Term.Sort.FLOAT32
        ? context.mkFP(Float.parseFloat(value), sort(sort))
        : context.mkFP(Double.parseDouble(value), sort(sort));
  }

  static boolean floating(Expr<?> value) {
    return value instanceof FPExpr;
  }

  FPSort common(Expr<?> left, Expr<?> right) {
    return left instanceof FPExpr a && a.getSort().getSBits() == 53
            || right instanceof FPExpr b && b.getSort().getSBits() == 53
        ? context.mkFPSort64()
        : context.mkFPSort32();
  }

  FPExpr convert(Expr<?> value, FPSort target) {
    if (value instanceof FPExpr fp)
      return fp.getSort().equals(target) ? fp : context.mkFPToFP(context.mkFPRNE(), fp, target);
    if (value instanceof IntExpr integer)
      return context.mkFPToFP(context.mkFPRNE(), context.mkInt2Real(integer), target);
    if (value instanceof RealExpr real) return context.mkFPToFP(context.mkFPRNE(), real, target);
    throw new IllegalArgumentException("Expected a numeric value, got " + value.getSort());
  }

  Expr<?> binary(String operator, Expr<?> left, Expr<?> right) {
    var sort = common(left, right);
    var a = convert(left, sort);
    var b = convert(right, sort);
    return switch (operator) {
      case "+" -> context.mkFPAdd(context.mkFPRNE(), a, b);
      case "-" -> context.mkFPSub(context.mkFPRNE(), a, b);
      case "*" -> context.mkFPMul(context.mkFPRNE(), a, b);
      case "/" -> context.mkFPDiv(context.mkFPRNE(), a, b);
      case "%" -> remainder(a, b);
      case "==" -> context.mkFPEq(a, b);
      case "!=" -> context.mkNot(context.mkFPEq(a, b));
      case "<" -> context.mkFPLt(a, b);
      case "<=" -> context.mkFPLEq(a, b);
      case ">" -> context.mkFPGt(a, b);
      case ">=" -> context.mkFPGEq(a, b);
      default -> throw new IllegalArgumentException("Invalid floating-point operator: " + operator);
    };
  }

  IntExpr truncate(RealExpr real) {
    return (IntExpr)
        context.mkITE(
            context.mkLt(real, context.mkReal(0)),
            context.mkUnaryMinus(context.mkReal2Int((RealExpr) context.mkUnaryMinus(real))),
            context.mkReal2Int(real));
  }

  IntExpr integer(FPExpr fp, int width) {
    var minimum = BigInteger.ONE.shiftLeft(width - 1).negate();
    var maximum = BigInteger.ONE.shiftLeft(width - 1).subtract(BigInteger.ONE);
    var real = context.mkFPToReal(fp);
    return (IntExpr)
        context.mkITE(
            context.mkFPIsNaN(fp),
            context.mkInt(0),
            context.mkITE(
                context.mkFPIsInfinite(fp),
                context.mkITE(
                    context.mkFPIsNegative(fp),
                    context.mkInt(minimum.toString()),
                    context.mkInt(maximum.toString())),
                context.mkITE(
                    context.mkLe(real, context.mkReal(minimum.toString())),
                    context.mkInt(minimum.toString()),
                    context.mkITE(
                        context.mkGe(real, context.mkReal(maximum.toString())),
                        context.mkInt(maximum.toString()),
                        truncate(real)))));
  }

  private FPExpr remainder(FPExpr a, FPExpr b) {
    // Java uses a quotient truncated toward zero, unlike IEEE remainder's nearest integer quotient.
    var x = context.mkFPToReal(a);
    var y = context.mkFPToReal(b);
    var quotient = truncate((RealExpr) context.mkDiv(x, y));
    var remainder = (RealExpr) context.mkSub(x, context.mkMul(context.mkInt2Real(quotient), y));
    var zero = context.mkFPZero(a.getSort(), false);
    var signedZero = context.mkITE(context.mkFPIsNegative(a), context.mkFPNeg(zero), zero);
    var finite =
        context.mkITE(
            context.mkEq(remainder, context.mkReal(0)),
            signedZero,
            context.mkFPToFP(context.mkFPRNE(), remainder, a.getSort()));
    var invalid =
        context.mkOr(
            context.mkFPIsNaN(a),
            context.mkFPIsNaN(b),
            context.mkFPIsInfinite(a),
            context.mkFPIsZero(b));
    return (FPExpr)
        context.mkITE(
            invalid,
            context.mkFPNaN(a.getSort()),
            context.mkITE(
                context.mkOr(context.mkFPIsZero(a), context.mkFPIsInfinite(b)), a, finite));
  }
}
