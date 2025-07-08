/**
 * @author Davin Yoon
 * CompilationEngine does the syntax analysis of a Jack file
 * It parses tokens from the JackTokenizer and writes cthe corresponding XML tags to the output
 */
package JackCompiler;

import java.io.*;


public class CompilationEngine {
    private JackTokenizer tokenizer;
    private PrintWriter writer;

    /**
     * Constructs the CompilationEngine, initializes tokenizer and output writer.
     */
    public CompilationEngine(File inputFile, File outputFile) throws IOException {
        tokenizer = new JackTokenizer(inputFile);
        writer = new PrintWriter(outputFile);
        compileClass(); 
        writer.close();
    }

    /**
     * Writes the full XML tag in the format: <tag> content </tag>
     */
    private void write(String tag, String content) {
        writer.printf("<%s> %s </%s>%n", tag, content, tag);
    }

    /**
     * Writes special XML symbols 
     */
    private void writeSymbol(char c) {
        switch (c) {
            case '<' -> write("symbol", "&lt;");
            case '>' -> write("symbol", "&gt;");
            case '&' -> write("symbol", "&amp;");
            default  -> write("symbol", String.valueOf(c));
        }
    }

    /**
     * Parses the entire class structure
     */
    public void compileClass() {
        writer.println("<class>");
        tokenizer.advance(); write("keyword", tokenizer.keyword()); // class
        tokenizer.advance(); write("identifier", tokenizer.identifier()); // class name
        tokenizer.advance(); writeSymbol(tokenizer.symbol()); // {

        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                String keyword = tokenizer.keyword();
                if (keyword.equals("static") || keyword.equals("field")) compileClassVarDec();
                else if (keyword.equals("constructor") || keyword.equals("function") || keyword.equals("method")) compileSubroutine();
                else if (keyword.equals("}")) { writeSymbol('}'); break; }
            }
        }
        writer.println("</class>");
    }

    /**
     * Compiles the class variable declarations
     */
    public void compileClassVarDec() {
        writer.println("<classVarDec>");
        write("keyword", tokenizer.keyword());
        tokenizer.advance(); write("keyword", tokenizer.keyword()); // type
        tokenizer.advance(); write("identifier", tokenizer.identifier()); // varName

        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.symbol() == ';') { writeSymbol(';'); break; }
            writeSymbol(tokenizer.symbol());
            tokenizer.advance(); write("identifier", tokenizer.identifier());
        }
        writer.println("</classVarDec>");
    }

    /**
     * Compiles subroutine declarations
     */
    public void compileSubroutine() {
        writer.println("<subroutineDec>");
        write("keyword", tokenizer.keyword());
        tokenizer.advance(); write(tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD ? "keyword" : "identifier", tokenizer.identifier()); // return type
        tokenizer.advance(); write("identifier", tokenizer.identifier()); // subroutine name
        tokenizer.advance(); writeSymbol(tokenizer.symbol()); // (
        compileParameterList();
        tokenizer.advance(); writeSymbol(tokenizer.symbol()); // )
        compileSubroutineBody();
        writer.println("</subroutineDec>");
    }

    /**
     * Compiles parameter list of a subroutine
     */
    public void compileParameterList() {
        writer.println("<parameterList>");
        tokenizer.advance();
        while (tokenizer.symbol() != ')') {
            write(tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD ? "keyword" : "identifier", tokenizer.identifier());
            tokenizer.advance(); write("identifier", tokenizer.identifier());
            tokenizer.advance();
            if (tokenizer.symbol() == ',') { writeSymbol(','); tokenizer.advance(); }
        }
        tokenizer.retreat(); // rollback from closing parenthesis
        writer.println("</parameterList>");
    }

    /**
     * Compiles the body of a subroutine
     */
    public void compileSubroutineBody() {
        writer.println("<subroutineBody>");
        tokenizer.advance(); writeSymbol(tokenizer.symbol()); // {
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.keyword().equals("var")) compileVarDec();
            else { tokenizer.retreat(); compileStatements(); break; }
        }
        tokenizer.advance(); writeSymbol(tokenizer.symbol()); // }
        writer.println("</subroutineBody>");
    }

    /**
     * Compiles variable declarations inside subroutines
     */
    public void compileVarDec() {
        writer.println("<varDec>");
        write("keyword", tokenizer.keyword());
        tokenizer.advance(); write(tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD ? "keyword" : "identifier", tokenizer.identifier());
        tokenizer.advance(); write("identifier", tokenizer.identifier());

        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.symbol() == ';') { writeSymbol(';'); break; }
            writeSymbol(tokenizer.symbol());
            tokenizer.advance(); write("identifier", tokenizer.identifier());
        }
        writer.println("</varDec>");
    }

    /**
     * Compiles a sequence of statements
     */
    public void compileStatements() {
        writer.println("<statements>");
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            switch (tokenizer.keyword()) {
                case "let" -> compileLet();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                default -> { tokenizer.retreat(); writer.println("</statements>"); return; }
            }
        }
        writer.println("</statements>");
    }

    /**
     * Compiles do statements
     */
    public void compileDo() { writer.println("<doStatement>"); advanceWriteTill(';'); writer.println("</doStatement>"); }

    /**
     * Compiles let statements
     */
    public void compileLet() { writer.println("<letStatement>"); advanceWriteTill(';'); writer.println("</letStatement>"); }

    /**
     * Compiles while statements
     */
    public void compileWhile() { writer.println("<whileStatement>"); advanceWriteTill('}'); writer.println("</whileStatement>"); }

    /**
     * Compiles return statements
     */
    public void compileReturn() { writer.println("<returnStatement>"); advanceWriteTill(';'); writer.println("</returnStatement>"); }

    /**
     * Compiles if statements
     */
    public void compileIf() { writer.println("<ifStatement>"); advanceWriteTill('}'); writer.println("</ifStatement>"); }

    /**
     * Continues writing tokens until a target symbol is found
     */
    private void advanceWriteTill(char endSymbol) {
        do {
            writeToken();
            tokenizer.advance();
        } while (tokenizer.symbol() != endSymbol);
        writeSymbol(tokenizer.symbol());
    }

    /**
     * Writes an expression
     */
    public void compileExpression() { writer.println("<expression>"); writeToken(); writer.println("</expression>"); }

    /**
     * Writes a term
     */
    public void compileTerm() { writer.println("<term>"); writeToken(); writer.println("</term>"); }

    /**
     * Writes an expression list
     */
    public void compileExpressionList() { writer.println("<expressionList>"); writeToken(); writer.println("</expressionList>"); }

    /**
     * Writes current token to XML based on its type
     */
    private void writeToken() {
        switch (tokenizer.tokenType()) {
            case KEYWORD -> write("keyword", tokenizer.keyword());
            case SYMBOL -> writeSymbol(tokenizer.symbol());
            case IDENTIFIER -> write("identifier", tokenizer.identifier());
            case INT_CONST -> write("integerConstant", String.valueOf(tokenizer.intVal()));
            case STRING_CONST -> write("stringConstant", tokenizer.stringVal());
        }
    }
}
