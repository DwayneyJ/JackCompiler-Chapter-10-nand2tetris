/**
 * @author Davin Yoon 
 * Chapter 10 nand2tetris
 * Main for the Jack Compiler.
 * Tokenizes a .jack file into an XML file.
 */
package JackCompiler;

import java.io.*;


public class Main {

    /**
     * Main method to run the tokenizer.
     * @param args The command line argument: inputFile.jack
     * @throws IOException if input/output operations fail
     */
    public static void main(String[] args) throws IOException {
        // Need 1 argument which is the jack file
        if (args.length != 1) {
            System.out.println("Usage: java JackCompiler.Main <inputFile.jack>");
            return;
        }

        // Check if input file is valid
        File inputFile = new File(args[0]);
        if (!inputFile.exists() || !inputFile.getName().endsWith(".jack")) {
            System.out.println("Error: Input file must be a valid .jack file.");
            return;
        }

        // Create output file replacing .jack with Token.xml
        File outputFile = new File(
            inputFile.getParent(), 
            inputFile.getName().replace(".jack", "Token.xml")
        );

        JackTokenizer tokenizer = new JackTokenizer(inputFile);
        PrintWriter writer = new PrintWriter(outputFile);

        // Tokenize the input file and write each token to the XML
        writer.println("<tokens>");
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            switch (tokenizer.tokenType()) {
                case KEYWORD -> writer.printf("<keyword> %s </keyword>%n", tokenizer.keyword());
                case SYMBOL -> {
                    // Handle special xml characters
                    char symbol = tokenizer.symbol();
                    String s = switch (symbol) {
                        case '<' -> "&lt;";
                        case '>' -> "&gt;";
                        case '&' -> "&amp;";
                        default -> String.valueOf(symbol);
                    };
                    writer.printf("<symbol> %s </symbol>%n", s);
                }
                // print the formatted tokens in xml format
                case IDENTIFIER -> writer.printf("<identifier> %s </identifier>%n", tokenizer.identifier());
                case INT_CONST -> writer.printf("<integerConstant> %d </integerConstant>%n", tokenizer.intVal());
                case STRING_CONST -> writer.printf("<stringConstant> %s </stringConstant>%n", tokenizer.stringVal());
            }
        }
        writer.println("</tokens>");
        writer.close();

        System.out.println("Token XML generated");
    }
}
