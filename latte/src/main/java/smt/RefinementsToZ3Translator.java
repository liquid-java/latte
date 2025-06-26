package smt;

import org.antlr.v4.runtime.tree.ParseTree;

import com.microsoft.z3.*;

import refinements.*;
import refinements.RefinementsLanguageParser.ProgContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The translator extends RefinementsLanguageBaseVisitor and converts parse tree nodes
 * to Z3 Expr objects (BoolExpr for boolean expressions, ArithExpr for arithmetic, etc.)
 */
public class RefinementsToZ3Translator extends RefinementsLanguageBaseVisitor<Expr> {
    private final Context z3Context;
    private final Map<String, Expr> variableMap;
    
    public RefinementsToZ3Translator(Context z3Context) {
        this.z3Context = z3Context;
        this.variableMap = new HashMap<>();
    }
    
    /**
     * Main entry point - translate a parse tree to a Z3 formula
     * This method is already complete.
     */
    public Expr translate(ParseTree tree) {
        Expr result = visit(tree);        
        if (result != null) {
            return result;
        } else {
            throw new IllegalArgumentException("Root expression must be expression, got: " + result.getClass());
        }
    }

    @Override
    public Expr visitProg(ProgContext ctx) {
        return super.visitProg(ctx);
    }

    // =================================================================
    // Implement visitor methods for elements in the grammar
    // =================================================================

    @Override
    public Expr visitLitBool(RefinementsLanguageParser.LitBoolContext ctx) {
        if (ctx.getText().equals("true")) {
            return z3Context.mkTrue();
        } else {
            return z3Context.mkFalse();
        }
    }

    @Override
    public Expr visitLitInt(RefinementsLanguageParser.LitIntContext ctx) {
        String intText = ctx.getText().replace("_", ""); // sanitize e.g., 1_000
        int value = Integer.parseInt(intText);
        return z3Context.mkInt(value);
    }

    @Override
    public Expr visitLit(RefinementsLanguageParser.LitContext ctx) {
        return visit(ctx.literal());  // Delegate to litBool or litInt
    }

    @Override
    public Expr visitVar(RefinementsLanguageParser.VarContext ctx) {
        String varName = ctx.getText();

        // Look up if we've already created a Z3 variable for this name
        if (variableMap.containsKey(varName)) {
            return variableMap.get(varName);
        }

        // If not, create a fresh Z3 integer variable (default assumption)
        // Later you could refine this to support booleans too if needed.
        Expr z3Var = z3Context.mkIntConst(varName);

        // Store it in the map so it’s reused consistently
        variableMap.put(varName, z3Var);

        return z3Var;
    }

    @Override
    public Expr visitOpArith(RefinementsLanguageParser.OpArithContext ctx) {
        // Visit the left and right operands
        Expr left = visit(ctx.operand(0));
        Expr right = visit(ctx.operand(1));
        String op = ctx.ARITHOP().getText();

        // Ensure both sides are arithmetic expressions
        if (!(left instanceof ArithExpr) || !(right instanceof ArithExpr)) {
            throw new IllegalArgumentException("Operands must be arithmetic expressions");
        }

        ArithExpr leftArith = (ArithExpr) left;
        ArithExpr rightArith = (ArithExpr) right;

        // Dispatch based on operator
        switch (op) {
            case "+":
                return z3Context.mkAdd(leftArith, rightArith);
            case "*":
                return z3Context.mkMul(leftArith, rightArith);
            case "/":
                return z3Context.mkDiv(leftArith, rightArith); // Integer division
            default:
                throw new UnsupportedOperationException("Unknown arithmetic operator: " + op);
        }
    }

    @Override
    public Expr visitExpBool(RefinementsLanguageParser.ExpBoolContext ctx) {
        Expr left = visit(ctx.exp(0));
        Expr right = visit(ctx.exp(1));
        String op = ctx.BOOLOP().getText();

        if (!(left instanceof ArithExpr) || !(right instanceof ArithExpr)) {
            throw new IllegalArgumentException("Both sides of a comparison must be arithmetic expressions.");
        }

        ArithExpr leftArith = (ArithExpr) left;
        ArithExpr rightArith = (ArithExpr) right;

        switch (op) {
            case "==":
                return z3Context.mkEq(leftArith, rightArith);
            case "<":
                return z3Context.mkLt(leftArith, rightArith);
            case "<=":
                return z3Context.mkLe(leftArith, rightArith);
            case ">":
                return z3Context.mkGt(leftArith, rightArith);
            case ">=":
                return z3Context.mkGe(leftArith, rightArith);
            default:
                throw new UnsupportedOperationException("Unknown boolean operator: " + op);
        }
    }

}