package latte.refinement;

import com.microsoft.z3.*;
import java.util.*;

/** Solver adapter. Only UNSAT proves an obligation; SAT and UNKNOWN are distinct failures. */
public final class Prover implements AutoCloseable {
  public record Function(List<Term.Sort> parameters, Term.Sort result) {
    public Function {
      parameters = List.copyOf(parameters);
    }
  }

  public record Proof(boolean proven, String detail) {}

  private final Context context = new Context();
  private final FloatingPoint floating = new FloatingPoint(context);
  private final Sort referenceSort = context.mkUninterpretedSort("JavaReference");
  private final Map<String, Function> functions;
  private final Map<String, FuncDecl<?>> declarations = new HashMap<>();

  public Prover(Map<String, Function> functions) {
    this.functions = Map.copyOf(functions);
  }

  public void validate(Term predicate) {
    bool(translate(predicate));
  }

  public Proof prove(Term assumptions, Term conclusion) {
    Solver solver = context.mkSolver();
    var parameters = context.mkParams();
    parameters.add("timeout", 2000);
    solver.setParameters(parameters);
    solver.add(
        new BoolExpr[] {bool(translate(assumptions)), context.mkNot(bool(translate(conclusion)))});
    var status = solver.check();
    return new Proof(
        status == Status.UNSATISFIABLE,
        status == Status.UNKNOWN
            ? "Solver could not decide: " + solver.getReasonUnknown()
            : status == Status.SATISFIABLE ? solver.getModel().toString() : "proved");
  }

  public Proof satisfiable(Term predicate) {
    var solver = context.mkSolver();
    var parameters = context.mkParams();
    parameters.add("timeout", 2000);
    solver.setParameters(parameters);
    solver.add(new BoolExpr[] {bool(translate(predicate))});
    var status = solver.check();
    return new Proof(
        status == Status.SATISFIABLE,
        status == Status.UNKNOWN ? solver.getReasonUnknown() : status.toString());
  }

  private Sort sort(Term.Sort sort) {
    return switch (sort) {
      case BOOL -> context.getBoolSort();
      case INT -> context.getIntSort();
      case REAL -> context.getRealSort();
      case FLOAT32, FLOAT64 -> floating.sort(sort);
      case REF -> referenceSort;
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Expr<?> translate(Term term) {
    if (term instanceof Term.Binary binary && Set.of("&&", "||").contains(binary.operator())) {
      // Constructor heaps and loop paths can accumulate thousands of assumptions. Flatten
      // associative boolean terms iteratively instead of exhausting the Java stack.
      var pending = new ArrayDeque<Term>();
      var operands = new ArrayList<BoolExpr>();
      pending.push(term);
      while (!pending.isEmpty()) {
        var next = pending.pop();
        if (next instanceof Term.Binary nested && nested.operator().equals(binary.operator())) {
          pending.push(nested.right());
          pending.push(nested.left());
        } else operands.add(bool(translate(next)));
      }
      var expressions = operands.toArray(BoolExpr[]::new);
      return binary.operator().equals("&&")
          ? context.mkAnd(expressions)
          : context.mkOr(expressions);
    }
    if (term instanceof Term.ArraySymbol array)
      return context.mkConst(
          array.name(), context.mkArraySort(context.getIntSort(), sort(array.elementSort())));
    if (term instanceof Term.ArrayConstant array)
      return context.mkConstArray(context.getIntSort(), translate(array.value()));
    if (term instanceof Term.ArrayStore array)
      return context.mkStore(
          (ArrayExpr) translate(array.array()),
          integer(translate(array.index())),
          translate(array.value()));
    if (term instanceof Term.ArraySelect array)
      return context.mkSelect(
          (ArrayExpr) translate(array.array()), integer(translate(array.index())));
    if (term instanceof Term.Literal literal)
      return switch (literal.sort()) {
        case BOOL -> context.mkBool(Boolean.parseBoolean(literal.value()));
        case INT -> context.mkInt(literal.value());
        case REAL -> context.mkReal(literal.value());
        case FLOAT32, FLOAT64 -> floating.literal(literal.value(), literal.sort());
        case REF -> context.mkConst("literal:" + literal.value(), referenceSort);
      };
    if (term instanceof Term.Symbol symbol)
      return context.mkConst(symbol.name(), sort(symbol.sort()));
    if (term instanceof Term.Name name)
      throw new IllegalArgumentException("Unknown refinement name: " + name.value());
    if (term instanceof Term.Choice choice) {
      var yes = translate(choice.yes());
      var no = translate(choice.no());
      if (FloatingPoint.floating(yes) || FloatingPoint.floating(no)) {
        var common = floating.common(yes, no);
        yes = floating.convert(yes, common);
        no = floating.convert(no, common);
      } else if (yes instanceof RealExpr && no instanceof IntExpr n) no = context.mkInt2Real(n);
      else if (no instanceof RealExpr && yes instanceof IntExpr n) yes = context.mkInt2Real(n);
      return context.mkITE(bool(translate(choice.condition())), yes, no);
    }
    if (term instanceof Term.Unary unary) {
      var operand = translate(unary.operand());
      if (operand instanceof FPExpr fp)
        return switch (unary.operator()) {
          case "-" -> context.mkFPNeg(fp);
          case "+" -> fp;
          default -> throw new IllegalArgumentException("Invalid floating unary operator");
        };
      return switch (unary.operator()) {
        case "!" -> context.mkNot(bool(operand));
        case "-" -> context.mkUnaryMinus(arith(operand));
        case "+" -> arith(operand);
        case "~" -> context.mkSub(context.mkUnaryMinus(arith(operand)), context.mkInt(1));
        default ->
            throw new IllegalArgumentException("Unsupported unary operation " + unary.operator());
      };
    }
    if (term instanceof Term.Call call) {
      if (call.name().startsWith("unbox:")) {
        var target = sort(Contracts.sort(call.name().substring(6)));
        var declaration =
            declarations.computeIfAbsent(
                call.name(), name -> context.mkFuncDecl(name, new Sort[] {referenceSort}, target));
        return context.mkApp(declaration, translate(call.arguments().get(0)));
      }
      if (call.name().startsWith("instanceof:")) {
        var declaration =
            declarations.computeIfAbsent(
                call.name(),
                name ->
                    context.mkFuncDecl(name, new Sort[] {referenceSort}, context.getBoolSort()));
        return context.mkApp(declaration, translate(call.arguments().get(0)));
      }
      if (call.name().startsWith("require:")) {
        var operand = translate(call.arguments().get(0));
        var target = Term.Sort.valueOf(call.name().substring(8));
        if (target == Term.Sort.FLOAT32 || target == Term.Sort.FLOAT64) {
          if (operand instanceof ArithExpr<?>
              || operand instanceof FPExpr
                  && (target == Term.Sort.FLOAT64
                      || operand.getSort().equals(sort(Term.Sort.FLOAT32))))
            return floating.convert(operand, floating.sort(target));
        } else if (operand.getSort().equals(sort(target))) return operand;
        throw new IllegalArgumentException(
            "Argument mismatch: expected " + target + ", got " + operand.getSort());
      }
      if (call.name().startsWith("cast:")) {
        var operand = translate(call.arguments().get(0));
        var target = call.name().substring(5);
        if (operand.getSort().equals(referenceSort)) {
          var declaration =
              declarations.computeIfAbsent(
                  "unbox:" + target,
                  name ->
                      context.mkFuncDecl(
                          name, new Sort[] {referenceSort}, sort(Contracts.sort(target))));
          operand = context.mkApp(declaration, operand);
        }
        if (target.equals("float") || target.equals("double"))
          return floating.convert(operand, floating.sort(Contracts.sort(target)));
        if (operand instanceof FPExpr fp)
          return floating.integer(fp, target.equals("long") ? 64 : 32);
        return operand;
      }
      if (call.name().startsWith("bit:")) {
        var parts = call.name().split(":");
        int width = Integer.parseInt(parts[2]);
        var left = context.mkInt2BV(width, integer(translate(call.arguments().get(0))));
        var count = integer(translate(call.arguments().get(1)));
        boolean shift = Set.of("SL", "SR", "USR").contains(parts[1]);
        var right =
            context.mkInt2BV(width, shift ? context.mkMod(count, context.mkInt(width)) : count);
        var result =
            switch (parts[1]) {
              case "BITAND" -> context.mkBVAND(left, right);
              case "BITOR" -> context.mkBVOR(left, right);
              case "BITXOR" -> context.mkBVXOR(left, right);
              case "SL" -> context.mkBVSHL(left, right);
              case "SR" -> context.mkBVASHR(left, right);
              case "USR" -> context.mkBVLSHR(left, right);
              default -> throw new IllegalArgumentException("Unknown bit operation");
            };
        return context.mkBV2Int(result, true);
      }
      var signature = functions.get(call.name());
      if (signature == null)
        throw new IllegalArgumentException("Unknown refinement function: " + call.name());
      if (signature.parameters().size() != call.arguments().size())
        throw new IllegalArgumentException("Wrong arity for " + call.name());
      var declaration =
          declarations.computeIfAbsent(
              call.name(),
              name ->
                  context.mkFuncDecl(
                      name,
                      signature.parameters().stream().map(this::sort).toArray(Sort[]::new),
                      sort(signature.result())));
      var arguments = new Expr<?>[call.arguments().size()];
      for (int i = 0; i < arguments.length; i++) {
        arguments[i] = translate(call.arguments().get(i));
        var expected = signature.parameters().get(i);
        if (expected == Term.Sort.FLOAT32 || expected == Term.Sort.FLOAT64)
          arguments[i] = floating.convert(arguments[i], floating.sort(expected));
        else if (expected == Term.Sort.REAL && arguments[i] instanceof IntExpr integer)
          arguments[i] = context.mkInt2Real(integer);
      }
      return declaration.apply(arguments);
    }
    var binary = (Term.Binary) term;
    var left = translate(binary.left());
    var right = translate(binary.right());
    if (binary.operator().equals("===")) return context.mkEq(left, right);
    if (FloatingPoint.floating(left) || FloatingPoint.floating(right))
      return floating.binary(binary.operator(), left, right);
    return switch (binary.operator()) {
      case "&&" -> context.mkAnd(bool(left), bool(right));
      case "||" -> context.mkOr(bool(left), bool(right));
      case "-->" -> context.mkImplies(bool(left), bool(right));
      case "<=>" -> context.mkIff(bool(left), bool(right));
      case "==" -> context.mkEq(left, right);
      case "!=" -> context.mkNot(context.mkEq(left, right));
      case "<" -> context.mkLt(arith(left), arith(right));
      case "<=" -> context.mkLe(arith(left), arith(right));
      case ">" -> context.mkGt(arith(left), arith(right));
      case ">=" -> context.mkGe(arith(left), arith(right));
      case "+" -> context.mkAdd(arith(left), arith(right));
      case "-" -> context.mkSub(arith(left), arith(right));
      case "*" -> context.mkMul(arith(left), arith(right));
      case "/" -> context.mkDiv(arith(left), arith(right));
      case "%" -> context.mkMod(integer(left), integer(right));
      default ->
          throw new IllegalArgumentException(
              "Unsupported refinement operator " + binary.operator());
    };
  }

  private static BoolExpr bool(Expr<?> value) {
    if (value instanceof BoolExpr result) return result;
    throw new IllegalArgumentException("Expected a boolean predicate, got " + value.getSort());
  }

  private static ArithExpr<?> arith(Expr<?> value) {
    if (value instanceof ArithExpr<?> result) return result;
    throw new IllegalArgumentException("Expected a number, got " + value.getSort());
  }

  private static IntExpr integer(Expr<?> value) {
    if (value instanceof IntExpr result) return result;
    throw new IllegalArgumentException("Expected an integer, got " + value.getSort());
  }

  @Override
  public void close() {
    context.close();
  }
}
