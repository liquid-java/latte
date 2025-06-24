package smt;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import com.microsoft.z3.*;

import refinements.*;

/**
 * Test class to verify  RefinementsToZ3Translator implementation
 * 
 * Run this class after implementing methods in the translator.
 * Start with simple tests and work your way up to complex ones.
 */
public class SimpleTranslatorTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Student Translator Implementation");
        System.out.println("========================================");
        
        // Test in order of difficulty
        testStep1_Literals();
        // testStep2_Variables();
        // testStep3_Arithmetic();
        // testStep4_Comparisons();
        // testStep5_LogicalOperations();
        // testStep6_Implications();
        // testStep7_ComplexExpressions();
    }
    
    public static void testStep1_Literals() {
        System.out.println("\n🔸 STEP 1: Testing Literals");
        System.out.println("Implement: visitIntegerLiteral, visitBooleanLiteral");
        
        testExpression("5", "Should create Z3 integer 5");
        testExpression("42", "Should create Z3 integer 42");
        testExpression("true", "Should create Z3 true");
        testExpression("false", "Should create Z3 false");
    }
    
    public static void testStep2_Variables() {
        System.out.println("\n🔸 STEP 2: Testing Variables");
        System.out.println("Implement: visitVariable");
        
        testExpression("x", "Should create Z3 variable x");
        testExpression("myVar", "Should create Z3 variable myVar");
    }
    
    public static void testStep3_Arithmetic() {
        System.out.println("\n🔸 STEP 3: Testing Arithmetic");
        System.out.println("Implement: visitArithmetic");
        
        testExpression("x + 5", "Should create addition");
        testExpression("10 - y", "Should create subtraction");
        testExpression("x * 2", "Should create multiplication");
    }
    
    public static void testStep4_Comparisons() {
        System.out.println("\n🔸 STEP 4: Testing Comparisons");
        System.out.println("Implement: visitEquality, visitRelational");
        
        testExpression("x == 5", "Should create equality");
        testExpression("x > 0", "Should create greater than");
        testExpression("y >= 10", "Should create greater or equal");
        testExpression("z < 0", "Should create less than");
        testExpression("w <= 5", "Should create less or equal");
    }
    
    public static void testStep5_LogicalOperations() {
        System.out.println("\n🔸 STEP 5: Testing Logical Operations");
        System.out.println("Implement: visitLogicalAnd, visitLogicalOr, visitLogicalNot");
        
        testExpression("x > 0 && y < 5", "Should create logical AND");
        testExpression("x == 0 || y == 0", "Should create logical OR");
        testExpression("!x > 0", "Should create logical NOT");
    }
    
    public static void testStep6_Implications() {
        System.out.println("\n🔸 STEP 6: Testing Implications");
        System.out.println("Implement: visitImplication");
        
        testExpression("x == 0 -> x > -1", "Should create implication");
        testExpression("x > 5 -> x > 0", "Should create implication");
    }
    
    public static void testStep7_ComplexExpressions() {
        System.out.println("\n🔸 STEP 7: Testing Complex Expressions");
        System.out.println("All methods should work together");
        
        testExpression("(x > 0 && y > 0) -> (x + y > 0)", "Complex implication");
        testExpression("x == 0 -> (x > -1 && x < 1)", "Implication with AND");
        testExpression("(x + y == 10) && (x > y)", "Conjunction of constraints");
    }
    
    /**
     * Test a single expression and report results
     */
    private static void testExpression(String expression, String description) {
        System.out.print("  Testing: " + expression + " (" + description + ") ... ");
        
        try (Context ctx = new Context()) {
            
            // Try to translate the expression
            BoolExpr result = translateExpression(expression, ctx);
            
            if (result != null) {
                System.out.println("✅ SUCCESS");
                System.out.println("    Z3 Formula: " + result);
                
                // For non-tautologies, try to find a model
                if (!expression.contains("->")) {
                    tryFindModel(result, ctx);
                }
            } else {
                System.out.println("❌ FAILED - Could not translate");
            }
            
        } catch (UnsupportedOperationException e) {
            System.out.println("⏳ TODO - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ ERROR - " + e.getMessage());
        }
    }
    
    /**
     * Translate expression using student's implementation
     */
    private static BoolExpr translateExpression(String expression, Context ctx) {
        try {
            // Parse with ANTLR
            CharStream input = CharStreams.fromString(expression);
            RefinementsLanguageLexer lexer = new RefinementsLanguageLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            RefinementsLanguageParser parser = new RefinementsLanguageParser(tokens);
            
            // Error handling
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                      int line, int charPositionInLine, String msg, RecognitionException e) {
                    throw new RuntimeException("Parse error: " + msg);
                }
            });
            
            // Parse
            ParseTree tree = parser.prog();
            System.out.println("\n  Parse Tree: " + tree.toStringTree(parser));
            
            // Translate using student's implementation
            RefinementsToZ3Translator translator = new RefinementsToZ3Translator(ctx);
            return translator.translate(tree);
            
        } catch (Exception e) {
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Try to find a satisfying model for the expression
     */
    private static void tryFindModel(BoolExpr formula, Context ctx) {
        try {
            Solver solver = ctx.mkSolver();
            solver.add(formula);
            
            System.out.println("Translation to SMT:\n"+solver.toString());
            Status result = solver.check();
            
            if (result == Status.SATISFIABLE) {
                Model model = solver.getModel();
                System.out.println("    Example solution: " + getModelString(model));
            } else if (result == Status.UNSATISFIABLE) {
                System.out.println("    (Unsatisfiable - no solutions exist)");
            }
            
        } catch (Exception e) {
            // Ignore model-finding errors
        }
    }
    
    /**
     * Convert model to readable string
     */
    private static String getModelString(Model model) {
        StringBuilder sb = new StringBuilder();
        FuncDecl[] decls = model.getDecls();
        
        for (int i = 0; i < decls.length; i++) {
            if (i > 0) sb.append(", ");
            String name = decls[i].getName().toString();
            Expr value = model.getConstInterp(decls[i]);
            sb.append(name).append("=").append(value);
        }
        
        return sb.toString();
    }
    
    /**
     * Bonus: Test validity of implications
     */
    public static void testValidityCheck() {
        System.out.println("\n🔸 BONUS: Testing Validity Checking");
        
        String[] implications = {
            "x == 0 -> x > -1",    // Should be valid
            "x > 5 -> x > 0",      // Should be valid  
            "x == 0 -> x < 0"      // Should be invalid
        };
        
        for (String impl : implications) {
            System.out.print("  Checking validity of: " + impl + " ... ");
            
            try (Context ctx = new Context()) {
                BoolExpr formula = translateExpression(impl, ctx);
                
                if (formula != null) {
                    Solver solver = ctx.mkSolver();
                    solver.add(ctx.mkNot(formula)); // Negate to check validity
                    
                    Status result = solver.check();
                    
                    if (result == Status.UNSATISFIABLE) {
                        System.out.println("✅ VALID (always true)");
                    } else {
                        System.out.println("❌ INVALID (counterexample exists)");
                    }
                } else {
                    System.out.println("❌ Could not translate");
                }
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
}