package smt;

import com.microsoft.z3.*;

/**
 * Simple Java program to test Z3 SMT solver with a specific implication.
 */
public class JavaSMTTest {
    public static void main(String[] args) {
        System.out.println("Testing Z3 Turnkey (with bundled natives)...");
        
        try {
            // Test the specific query: x == 0 -> x > -1
            testImplication();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void testImplication() {
        System.out.println("Checking: x == 0 -> x > -1");
        // Translation to z3 of the expression above is the following:
        
        try (Context ctx = new Context()) {
            
            // Create integer variable x
            IntExpr x = ctx.mkIntConst("x");
            
            // Create the expressions
            BoolExpr xEquals0 = ctx.mkEq(x, ctx.mkInt(0));          // x == 0
            BoolExpr xGreaterThanMinus1 = ctx.mkGt(x, ctx.mkInt(-1)); // x > -1
            
            // Create the implication: x == 0 -> x > -1
            BoolExpr implication = ctx.mkImplies(xEquals0, xGreaterThanMinus1);
            
            // Create solver
            Solver solver = ctx.mkSolver();
            
            // Check if implication is valid (tautology)
            // We do this by checking if the negation is unsatisfiable
            solver.add(ctx.mkNot(implication));
            
            Status result = solver.check();
            
            System.out.println("Result: " + result);
            
            if (result == Status.UNSATISFIABLE) {
                System.out.println("✓ VALID: The implication x == 0 -> x > -1 is always true");
            } else if (result == Status.SATISFIABLE) {
                System.out.println("✗ INVALID: Found a counterexample");
                Model model = solver.getModel();
                System.out.println("Counterexample: x = " + model.evaluate(x, false));
            } else {
                System.out.println("? UNKNOWN");
            }
            
            // Also test satisfiability of the implication itself
            System.out.println("\nTesting satisfiability of the implication...");
            Solver solver2 = ctx.mkSolver();
            solver2.add(implication);
            
            Status sat_result = solver2.check();
            System.out.println("Satisfiability: " + sat_result);
            
            if (sat_result == Status.SATISFIABLE) {
                Model model = solver2.getModel();
                System.out.println("Example where implication holds: x = " + model.evaluate(x, false));
            }
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Test multiple implications
    public static void testMultipleImplications() {
        System.out.println("\n=== Testing Multiple Implications ===");
        
        String[] queries = {
            "x == 0 -> x > -1",
            "x > 5 -> x > 0",
            "x == 0 -> x < 0"
        };
        
        for (String query : queries) {
            System.out.println("\nTesting: " + query);
            boolean result = checkImplication(query);
            System.out.println("Result: " + (result ? "VALID" : "INVALID"));
        }
    }
    
    private static boolean checkImplication(String description) {
        try (Context ctx = new Context()) {
            IntExpr x = ctx.mkIntConst("x");
            BoolExpr implication = null;
            
            // Build specific implications
            if (description.contains("x == 0 -> x > -1")) {
                implication = ctx.mkImplies(
                    ctx.mkEq(x, ctx.mkInt(0)),
                    ctx.mkGt(x, ctx.mkInt(-1))
                );
            } else if (description.contains("x > 5 -> x > 0")) {
                implication = ctx.mkImplies(
                    ctx.mkGt(x, ctx.mkInt(5)),
                    ctx.mkGt(x, ctx.mkInt(0))
                );
            } else if (description.contains("x == 0 -> x < 0")) {
                implication = ctx.mkImplies(
                    ctx.mkEq(x, ctx.mkInt(0)),
                    ctx.mkLt(x, ctx.mkInt(0))
                );
            }
            
            if (implication == null) return false;
            
            Solver solver = ctx.mkSolver();
            solver.add(ctx.mkNot(implication));
            
            Status result = solver.check();
            
            if (result == Status.SATISFIABLE) {
                Model model = solver.getModel();
                System.out.println("  Counterexample: x = " + model.evaluate(x, false));
                return false;
            }
            
            return result == Status.UNSATISFIABLE;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }
}