package com.example.metta.text;

import com.example.metta.atom.*;

import java.io.Reader; // Keep for future use
import java.io.StringReader; // Keep for future use
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.Objects; // For requireNonNull

public class SExprParser {

    private enum TokenType {
        OPEN_PAREN, CLOSE_PAREN, SYMBOL, VARIABLE, STRING_LITERAL, NUMBER_LITERAL
    }

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + "(\"" + value + "\")";
        }
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        // Order of patterns is important
        String tokenPatternStr = 
            "\\s+|" +                           // Whitespace (to be ignored)
            "\\(|" +                            // Open Paren
            "\\)|" +                            // Close Paren
            "\"(?:\\\\\"|[^\"])*\"|" +          // String Literal (handles escaped quotes)
            "\\$[A-Za-z_][A-Za-z0-9_\\-]*|" +   // Variable
            "[+-]?(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?|" + // Float (e.g. 3.14, -0.5, .5, 1.2e3)
            "[+-]?\\d+(?:[eE][+-]?\\d+)?|" +    // Integer (e.g. 123, -42, 1e3) - can also be part of float, hence order
            "[^\\s()\"'$#;][^\\s()\"]*|" +       // Symbol (general, avoids starting with special chars handled elsewhere, or comment char)
            "[\\-+*/<>=%?!&|.:;,@#^~]+";       // Symbol (common operator/punctuation like chars)
            // Note: Comments (e.g. ; to EOL) are not explicitly handled here but could be added to whitespace or as a token type.
            // For now, symbols starting with ';' would be caught if not whitespace separated.

        Pattern pattern = Pattern.compile(tokenPatternStr);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String match = matcher.group(0);
            if (match.matches("\\s+")) { // Skip whitespace
                continue;
            }
            if (match.equals("(")) {
                tokens.add(new Token(TokenType.OPEN_PAREN, match));
            } else if (match.equals(")")) {
                tokens.add(new Token(TokenType.CLOSE_PAREN, match));
            } else if (match.startsWith("\"") && match.endsWith("\"")) {
                String unescaped = match.substring(1, match.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                tokens.add(new Token(TokenType.STRING_LITERAL, unescaped));
            } else if (match.startsWith("$")) {
                tokens.add(new Token(TokenType.VARIABLE, match.substring(1))); 
            } else if (match.matches("[+-]?(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?") || 
                       match.matches("[+-]?\\d+[eE][+-]?\\d+")) { // Check for float or scientific notation
                tokens.add(new Token(TokenType.NUMBER_LITERAL, match)); 
            } else if (match.matches("[+-]?\\d+")) { // Integer
                tokens.add(new Token(TokenType.NUMBER_LITERAL, match)); 
            } else {
                tokens.add(new Token(TokenType.SYMBOL, match));
            }
        }
        return tokens;
    }

    private LinkedList<Token> tokens; 

    public SExprParser() {
    }

    public List<Atom> parseToList(String input) {
        Objects.requireNonNull(input, "Input string cannot be null");
        this.tokens = new LinkedList<>(tokenize(input));
        List<Atom> atoms = new ArrayList<>();
        while (!this.tokens.isEmpty()) {
            atoms.add(parseInternal());
        }
        return atoms;
    }
    
    public Atom parse(String input) {
        List<Atom> results = parseToList(input);
        if (results.isEmpty()) {
            throw new SExprParserException("No expression found in input: \"" + input + "\"");
        }
        if (results.size() > 1) {
            throw new SExprParserException("Multiple top-level expressions found ("+ results.size() +") in input: \"" + input + "\", use parseToList.");
        }
        return results.get(0);
    }

    // TODO: public List<Atom> parseToList(Reader reader)

    private Atom parseInternal() {
        if (tokens.isEmpty()) {
            throw new SExprParserException("Unexpected end of input while parsing.");
        }
        Token token = tokens.removeFirst();

        switch (token.type) {
            case OPEN_PAREN:
                List<Atom> children = new ArrayList<>();
                while (!tokens.isEmpty() && tokens.peekFirst().type != TokenType.CLOSE_PAREN) {
                    children.add(parseInternal());
                }
                if (tokens.isEmpty() || tokens.removeFirst().type != TokenType.CLOSE_PAREN) {
                    throw new SExprParserException("Mismatched parentheses: missing ')'");
                }
                return new ExpressionAtom(children);
            case CLOSE_PAREN:
                throw new SExprParserException("Unexpected ')'");
            case SYMBOL:
                // Check for known operational symbols first
                if (token.value.equals("+")) {
                    return new AddAtom();
                } else if (token.value.equals("-")) {
                    return new SubtractAtom();
                } else if (token.value.equals("*")) {
                    return new MultiplyAtom();
                }
                // Default to SymbolAtom if not a special operational symbol
                return new SymbolAtom(token.value);
            case VARIABLE:
                return new VariableAtom(token.value);
            case STRING_LITERAL:
                return new SymbolAtom(token.value); // As per current plan
            case NUMBER_LITERAL:
                try {
                    if (token.value.contains(".") || token.value.toLowerCase().contains("e")) {
                        return new GroundedAtom(Double.parseDouble(token.value));
                    } else {
                        return new GroundedAtom(Long.parseLong(token.value)); // Use Long for wider range
                    }
                } catch (NumberFormatException e) {
                    throw new SExprParserException("Invalid number format: " + token.value, e);
                }
            default:
                throw new SExprParserException("Unknown token type encountered: " + token.type);
        }
    }

    public static class SExprParserException extends RuntimeException {
        public SExprParserException(String message) {
            super(message);
        }
        public SExprParserException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Main method removed for brevity in final code, was for testing.
}
