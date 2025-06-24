package smt;

import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class JavaSMTTest {
    public static void main(String[] args) {
        System.out.println("Testing JavaSMT with bundled Z3...");
        
        try {
            // Test the specific query: x == 0 -> x > -1
            testImplication();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void testImplication() throws Exception {
        System.out.println("Checking: x == 0 -> x > -1");
        
        // Create solver context with Z3 (bundled, no installation needed)
        try (SolverContext context = SolverContextFactory.createSolverContext(
                SolverContextFactory.Solvers.Z3)) {
            
            // Get formula manager
            FormulaManager fmgr = context.getFormulaManager();
            IntegerFormulaManager ifmgr = fmgr.getIntegerFormulaManager();
            BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
            
            // Create integer variable x
            IntegerFormula x = ifmgr.makeVariable("x");
            
            // Create the expressions
            BooleanFormula xEquals0 = ifmgr.equal(x, ifmgr.makeNumber(0));           // x == 0
            BooleanFormula xGreaterThanMinus1 = ifmgr.greaterThan(x, ifmgr.makeNumber(-1)); // x > -1
            
            // Create the implication: x == 0 -> x > -1
            BooleanFormula implication = bfmgr.implication(xEquals0, xGreaterThanMinus1);
            
            // Create prover (for checking validity)
            try (ProverEnvironment prover = context.newProverEnvironment()) {
                
                // Check if implication is valid by checking if its negation is unsat
                prover.push(bfmgr.not(implication));
                
                boolean isUnsat = prover.isUnsat();
                
                if (isUnsat) {
                    System.out.println("✓ VALID: The implication x == 0 -> x > -1 is always true");
                } else {
                    System.out.println("✗ INVALID: The implication is not always true");
                    
                    // Get counterexample
                    try (Model model = prover.getModel()) {
                        System.out.println("Counterexample: x = " + model.evaluate(x));
                    }
                }
                
                prover.pop();
            }
            
            // Also test satisfiability of the implication itself
            System.out.println("\nTesting satisfiability of the implication...");
            try (ProverEnvironment prover = context.newProverEnvironment()) {
                prover.push(implication);
                
                boolean isSat = !prover.isUnsat();
                System.out.println("Satisfiability: " + (isSat ? "SAT" : "UNSAT"));
                
                if (isSat) {
                    try (Model model = prover.getModel()) {
                        System.out.println("Example where implication holds: x = " + model.evaluate(x));
                    }
                }
            }
        }
    }
    
    // Additional method to test multiple queries
    public static void testMultipleQueries() throws Exception {
        System.out.println("\n=== Testing Multiple Queries ===");
        
        try (SolverContext context = SolverContextFactory.createSolverContext(Solvers.Z3)) {
            FormulaManager fmgr = context.getFormulaManager();
            IntegerFormulaManager ifmgr = fmgr.getIntegerFormulaManager();
            BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
            
            IntegerFormula x = ifmgr.makeVariable("x");
            
            // Test different implications
            String[] descriptions = {
                "x == 0 -> x > -1",
                "x > 5 -> x > 0", 
                "x == 0 -> x < 0"
            };
            
            BooleanFormula[] implications = {
                // x == 0 -> x > -1
                bfmgr.implication(
                    ifmgr.equal(x, ifmgr.makeNumber(0)),
                    ifmgr.greaterThan(x, ifmgr.makeNumber(-1))
                ),
                // x > 5 -> x > 0
                bfmgr.implication(
                    ifmgr.greaterThan(x, ifmgr.makeNumber(5)),
                    ifmgr.greaterThan(x, ifmgr.makeNumber(0))
                ),
                // x == 0 -> x < 0
                bfmgr.implication(
                    ifmgr.equal(x, ifmgr.makeNumber(0)),
                    ifmgr.lessThan(x, ifmgr.makeNumber(0))
                )
            };
            
            for (int i = 0; i < descriptions.length; i++) {
                System.out.println("\nTesting: " + descriptions[i]);
                
                try (ProverEnvironment prover = context.newProverEnvironment()) {
                    prover.push(bfmgr.not(implications[i]));
                    boolean isValid = prover.isUnsat();
                    
                    System.out.println("Result: " + (isValid ? "VALID" : "INVALID"));
                    
                    if (!isValid) {
                        try (Model model = prover.getModel()) {
                            System.out.println("Counterexample: x = " + model.evaluate(x));
                        }
                    }
                }
            }
        }
    }
}