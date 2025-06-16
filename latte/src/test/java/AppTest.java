import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import api.App;
import context.SymbolicEnvironment;
import context.SymbolicValue;
import typechecking.LatteException;

public class AppTest {

    /**
     * Provides test cases for correct examples.
     * Each test case contains the file path of a valid example.
     * The expected outcome is that the test should pass (true).
     * 
     * @return Stream of Arguments, each containing a file path
     */
    private static Stream<Arguments> provideCorrectTestCases() {
        return Stream.of(
            Arguments.of("src/test/examples/MyNodeCorrect.java"),
            Arguments.of("src/test/examples/MyNodePush.java"),
            Arguments.of("src/test/examples/MyNodePushPop.java"),
            Arguments.of("src/test/examples/MyNodeComplete.java"),
            Arguments.of("src/test/examples/MyStackFieldAssign.java"),
            // Arguments.of("src/test/examples/BoxMain.java"), // TODO: Fix this example
            Arguments.of("src/test/examples/HttpEntityNoAnnotations.java"),
            Arguments.of("src/test/examples/searching_state_space/URLConnectionReuseConnection.java"),
            Arguments.of("src/test/examples/searching_state_space/URLConnectionSetProperty1.java"),
            Arguments.of("src/test/examples/searching_state_space/URLConnectionSetPropertyMultipleShort.java"),
            Arguments.of("src/test/examples/searching_state_space/TimerTaskCannotReschedule.java"),
            Arguments.of("src/test/examples/searching_state_space/ResultSetNoNext.java"),
            Arguments.of("src/test/examples/searching_state_space/ResultSetForwardOnly.java"),
            Arguments.of("src/test/examples/stack_overflow/MediaRecord.java"),
            Arguments.of("src/test/examples/MyNodeInvocationIf.java"),
            Arguments.of("src/test/examples/MyNodeIfInvocationPermission.java")
        );
    }

    /**
     * Provides test cases for incorrect examples.
     * Each test case contains the file path of an invalid example, the expected error message.
     * The expected outcome is that the test should fail (false), and the error message should match the expected one.
     * 
     * @return Stream of Arguments, each containing a file path and expected error message
     */
    private static Stream<Arguments> provideIncorrectTestCases() {
        return Stream.of(
            Arguments.of("src/test/examples/MyNode.java", "UNIQUE but got BORROWED"),
            // Arguments.of("src/test/examples/MyNodePushPopIncorrect.java", "FREE but got BOTTOM"), //TODO: Fix this example
            // Arguments.of("src/test/examples/MyNodeNoDistinct.java", "Non-distinct parameters"), //TODO: Fix this example
            Arguments.of("src/test/examples/MyNodeCallUniqueFree.java", "FREE but got UNIQUE"),
            Arguments.of("src/test/examples/SmallestIncorrectExample.java", "UNIQUE but got BORROWED"),
            Arguments.of("src/test/examples/MyStackFieldAssignMethod.java", "UNIQUE but got SHARED"),
            Arguments.of("src/test/examples/FieldAccessNoThis.java", "UNIQUE but got SHARED"),
            Arguments.of("src/test/examples/FieldAccessRightNoThis.java", "FREE but got UNIQUE")
        );
    }

    /**
     * Tests the correct examples.
     * Uses the {@link #provideCorrectTestCases} method to provide test data and verifies that the application
     * behaves correctly for each valid input file.
     * 
     * @param filePath the file path to the correct example
     */
    @ParameterizedTest
    @MethodSource("provideCorrectTestCases")
    public void testCorrectApp(String filePath) {
        runTest(filePath, true, null);
    }

    /**
     * Tests the incorrect examples.
     * Uses the {@link #provideIncorrectTestCases} method to provide test data and verifies that the application
     * fails with the expected error message for each invalid input file.
     * 
     * @param filePath the file path to the incorrect example
     * @param expectedErrorMessage the expected error message to be thrown
     */
    @ParameterizedTest
    @MethodSource("provideIncorrectTestCases")
    public void testIncorrectApp(String filePath, String expectedErrorMessage) {
        runTest(filePath, false, expectedErrorMessage);
    }

    /**
     * Runs the actual test logic for both correct and incorrect test cases.
     * For correct test cases, it ensures the application runs without errors.
     * For incorrect test cases, it ensures the application throws the expected exception with the correct error message.
     * 
     * @param filePath the file path to the test file
     * @param shouldPass true if the test should pass, false if it should fail
     * @param expectedErrorMessage the expected error message (used only for incorrect test cases)
     */
    private void runTest(String filePath, boolean shouldPass, String expectedErrorMessage) {
        try {
            App.launcher(filePath, false);
            if (!shouldPass) {
                fail("Expected an exception but none was thrown. File: " + filePath);
            }
        } catch (Exception e) {
            if (shouldPass) {
                fail("Unexpected exception: " + e.getMessage() + " File: " + filePath);
            } else {
                assertTrue(e instanceof LatteException, "Expected a LatteException but got: " + e.getClass().getName() + " File: " + filePath);
                // Print the exception message for debugging
                assertTrue(e.getMessage().contains(expectedErrorMessage), "Got message: " + e.getMessage() 
                + " but expected: " + expectedErrorMessage + " File: " + filePath);
            }
        }
    }

    /**
     * Unit test to check reachability between symbolic values in a symbolic environment.
     * Creates a symbolic environment, adds variables and fields, and tests if one variable can reach another.
     */
    @Test
    public void testReachabilityUnitTest() {
        Logger logger = Logger.getLogger(AppTest.class.getName());

        // Create a symbolic environment
        SymbolicEnvironment se = new SymbolicEnvironment();
        se.enterScope();

        SymbolicValue v1 = se.addVariable("x");
        // x->1
        SymbolicValue v2 = se.addVariable("y");
        // x->1; y->2
        SymbolicValue v3 = se.addField(v1, "f");
        // x->1; y->2, 1.f->3
        se.addVarSymbolicValue("z", v1);
        SymbolicValue v4 = se.get("z");
        // x->1; y->2, 1.f->3, z -> 1
        logger.info(se.toString());

        // Test reachability between variables
        boolean b = se.canReach(v1, v2, new ArrayList<>());
        logger.info(v1.toString() + " can reach " + v2.toString() + "? " + b);
        assertFalse(b);

        boolean b1 = se.canReach(v1, v3, new ArrayList<>());
        logger.info(v1.toString() + " can reach " + v3.toString() + "? " + b1);
        assertTrue(b1);

        boolean b2 = se.canReach(v1, v4, new ArrayList<>());
        logger.info(v1.toString() + " can reach " + v4.toString() + "? " + b2);
        assertTrue(b2);
    }
}
