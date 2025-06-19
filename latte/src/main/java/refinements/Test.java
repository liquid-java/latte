package refinements;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
// import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// import refinements.antlr.RefinementsLanguageLexer;
// import refinements.antlr.RefinementsLanguageParser;

public class Test {

    public static void main(String[] args) {
        String input = "a + b < 10 + c.y";

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
