package refinements;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Test class for the Refinements Language parser.
 * This class demonstrates how to parse a simple expression using ANTLR.
 */
public class TestRefinementsLanguage {

    public static void main(String[] args) {
        String input = "a < 10";

        // // Create input stream (modern way)
        CharStream inputStream = CharStreams.fromString(input);
        
        // Create lexer
        RefinementsLanguageLexer lexer = new RefinementsLanguageLexer(inputStream);
        
        // Create token stream
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        // Create parser
        RefinementsLanguageParser parser = new RefinementsLanguageParser(tokens);
        
        // Parse starting from your grammar's start rule
        // Replace 'program' with your actual start rule name
        ParseTree tree = parser.prog(); // Change 'program' to your start rule
        
        // Print the parse tree
        System.out.println("Parse Tree:");
        System.out.println(tree.toStringTree(parser));
        
    }
    
}
