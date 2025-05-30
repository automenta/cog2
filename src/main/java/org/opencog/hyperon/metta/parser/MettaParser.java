package org.opencog.hyperon.metta.parser;

import org.opencog.hyperon.atoms.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Deque;
import java.util.Iterator;


public class MettaParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\s*([()]|\\$[\\w-]+|[\\w-]+)\\s*");

    public List<Atom> parseScript(String scriptInput) {
        if (scriptInput == null || scriptInput.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = tokenize(scriptInput);
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Atom> atoms = new ArrayList<>();
        Deque<String> tokenQueue = new LinkedList<>(tokens);
        while (!tokenQueue.isEmpty()) {
            atoms.add(parseNextToken(tokenQueue));
        }
        return atoms;
    }

    public Atom parseAtom(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty.");
        }
        List<String> tokens = tokenize(input);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Input string produces no tokens.");
        }
        Deque<String> tokenQueue = new LinkedList<>(tokens);
        Atom atom = parseNextToken(tokenQueue);
        if (!tokenQueue.isEmpty()) {
            throw new MettaParserException("Extra tokens found after parsing atom: " + String.join(" ", tokenQueue));
        }
        return atom;
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                 String unrecognized = input.substring(lastEnd, matcher.start()).trim();
                 if (!unrecognized.isEmpty()) {
                    throw new MettaParserException("Unrecognized token: " + unrecognized);
                 }
            }
            tokens.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        if (lastEnd != input.length()) {
            String trailing = input.substring(lastEnd).trim();
            if (!trailing.isEmpty()) {
                 throw new MettaParserException("Trailing characters that could not be tokenized: " + trailing);
            }
        }
        return tokens;
    }

    private Atom parseNextToken(Deque<String> tokens) {
        if (tokens.isEmpty()) {
            throw new MettaParserException("Unexpected end of input");
        }
        String token = tokens.pop();
        if (token.equals("(")) {
            return parseExpressionTokens(tokens);
        } else if (token.equals(")")) {
            throw new MettaParserException("Unexpected closing parenthesis");
        } else if (token.startsWith("$")) {
            return new VariableAtom(token);
        } else {
            return new SymbolAtom(token);
        }
    }

    private ExpressionAtom parseExpressionTokens(Deque<String> tokens) {
        List<Atom> children = new ArrayList<>();
        while (true) {
            if (tokens.isEmpty()) {
                throw new MettaParserException("Unbalanced parentheses: missing closing parenthesis");
            }
            String nextTokenPeek = tokens.peek();
            if (nextTokenPeek.equals(")")) {
                tokens.pop(); // Consume the closing parenthesis
                break;
            }
            children.add(parseNextToken(tokens));
        }
        return new ExpressionAtom(children);
    }

    public static class MettaParserException extends RuntimeException {
        public MettaParserException(String message) {
            super(message);
        }
    }
}
