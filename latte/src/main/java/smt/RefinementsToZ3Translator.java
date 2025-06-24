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
    public BoolExpr translate(ParseTree tree) {
        Expr result = visit(tree);        
        if (result instanceof BoolExpr) {
            return (BoolExpr) result;
        } else {
            throw new IllegalArgumentException("Root expression must be boolean, got: " + result.getClass());
        }
    }

    @Override
    public Expr visitProg(ProgContext ctx) {
        return super.visitProg(ctx);
    }

    // =================================================================
    // Implement visitor methods for elements in the grammar
    // =================================================================

    public Expr visitLitBool(RefinementsLanguageParser.ProgContext ctx) {
        System.out.println("Visiting boolean literal: " + ctx.getText());
        // Handle boolean literals (true/false)
        String text = ctx.getText();
        if (text.equals("true")) {
            return z3Context.mkTrue();
        } else if (text.equals("false")) {
            return z3Context.mkFalse();
        } else 
            throw new IllegalArgumentException("Unknown boolean literal: " + text);
    }


}