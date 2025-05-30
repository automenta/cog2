package org.opencog.hyperon.metta.parser;

import org.opencog.hyperon.atoms.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MettaParserTest {

    private final MettaParser parser = new MettaParser();

    @Test
    void testParseSymbol() {
        Atom atom = parser.parseAtom("my-symbol");
        assertTrue(atom instanceof SymbolAtom);
        assertEquals("my-symbol", atom.getName());
        assertEquals(Atom.AtomType.SYMBOL, atom.getType());
    }

    @Test
    void testParseVariable() {
        Atom atom = parser.parseAtom("$var1");
        assertTrue(atom instanceof VariableAtom);
        assertEquals("$var1", atom.getName());
        assertEquals(Atom.AtomType.VARIABLE, atom.getType());
    }

    @Test
    void testParseSimpleExpression() {
        Atom atom = parser.parseAtom("(a b c)");
        assertTrue(atom instanceof ExpressionAtom);
        assertEquals(Atom.AtomType.EXPRESSION, atom.getType());
        List<Atom> children = atom.getChildren();
        assertEquals(3, children.size());
        assertEquals(new SymbolAtom("a"), children.get(0));
        assertEquals(new SymbolAtom("b"), children.get(1));
        assertEquals(new SymbolAtom("c"), children.get(2));
    }

    @Test
    void testParseNestedExpression() {
        Atom atom = parser.parseAtom("(a (b $c) d)");
        assertTrue(atom instanceof ExpressionAtom);
        List<Atom> children = atom.getChildren();
        assertEquals(3, children.size());
        assertEquals(new SymbolAtom("a"), children.get(0));
        assertTrue(children.get(1) instanceof ExpressionAtom);
        assertEquals(new SymbolAtom("d"), children.get(2));

        ExpressionAtom nestedExpr = (ExpressionAtom) children.get(1);
        List<Atom> nestedChildren = nestedExpr.getChildren();
        assertEquals(2, nestedChildren.size());
        assertEquals(new SymbolAtom("b"), nestedChildren.get(0));
        assertEquals(new VariableAtom("$c"), nestedChildren.get(1));
    }

    @Test
    void testParseMultipleExpressions() {
        List<Atom> atoms = parser.parseScript("sym (a b) $var (e (f g) h)");
        assertEquals(4, atoms.size());
        assertEquals(new SymbolAtom("sym"), atoms.get(0));
        assertEquals(new ExpressionAtom(Arrays.asList(new SymbolAtom("a"), new SymbolAtom("b"))), atoms.get(1));
        assertEquals(new VariableAtom("$var"), atoms.get(2));

        ExpressionAtom fourthAtom = (ExpressionAtom) atoms.get(3);
        assertEquals(new SymbolAtom("e"), fourthAtom.getChildren().get(0));
        assertTrue(fourthAtom.getChildren().get(1) instanceof ExpressionAtom);
        assertEquals(new SymbolAtom("h"), fourthAtom.getChildren().get(2));

        ExpressionAtom nestedInFourth = (ExpressionAtom) fourthAtom.getChildren().get(1);
        assertEquals(new SymbolAtom("f"), nestedInFourth.getChildren().get(0));
        assertEquals(new SymbolAtom("g"), nestedInFourth.getChildren().get(1));
    }

    @Test
    void testParseScriptWithLeadingAndTrailingSpaces() {
        List<Atom> atoms = parser.parseScript("  sym (a b) $var  ");
        assertEquals(3, atoms.size());
        assertEquals(new SymbolAtom("sym"), atoms.get(0));
        assertEquals(new ExpressionAtom(Arrays.asList(new SymbolAtom("a"), new SymbolAtom("b"))), atoms.get(1));
        assertEquals(new VariableAtom("$var"), atoms.get(2));
    }


    @Test
    void testParseEmptyExpression() {
        Atom atom = parser.parseAtom("()");
        assertTrue(atom instanceof ExpressionAtom);
        assertTrue(atom.getChildren().isEmpty());
    }

    @Test
    void testParseScriptWithEmptyExpression() {
        List<Atom> atoms = parser.parseScript("a () b");
        assertEquals(3, atoms.size());
        assertEquals(new SymbolAtom("a"), atoms.get(0));
        assertEquals(new ExpressionAtom(Collections.emptyList()), atoms.get(1));
        assertEquals(new SymbolAtom("b"), atoms.get(2));
    }


    @Test
    void testErrorUnbalancedParensMissingClosing() {
        MettaParser.MettaParserException exception =
                assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom("(a (b c)"));
        assertEquals("Unbalanced parentheses: missing closing parenthesis", exception.getMessage());
    }

    @Test
    void testErrorUnbalancedParensMissingOpening() {
        // This test checks for an unexpected closing parenthesis when parsing a single atom context.
        // Input "a b) c" is better tested with parseScript, or if parseAtom,
        // it correctly identifies "a" and then "b ) c" as extra tokens.
        // The simplest case for "Unexpected closing parenthesis" for parseAtom is just ")".
        MettaParser.MettaParserException exception =
                assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom(")"));
        assertEquals("Unexpected closing parenthesis", exception.getMessage());
    }


    @Test
    void testErrorExtraClosingParen() {
        MettaParser.MettaParserException exception =
                assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom("(a b c))"));
        assertEquals("Extra tokens found after parsing atom: )", exception.getMessage());
    }

    @Test
    void testErrorExtraTokensAfterExpression() {
        MettaParser.MettaParserException exception =
                assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom("(a b) c"));
        assertEquals("Extra tokens found after parsing atom: c", exception.getMessage());
    }

    @Test
    void testErrorUnrecognizedToken() {
        MettaParser.MettaParserException exception =
            assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom("(a % b)"));
        assertTrue(exception.getMessage().contains("Unrecognized token: %"));
    }

    @Test
    void testErrorTrailingUnrecognizedToken() {
        MettaParser.MettaParserException exception =
            assertThrows(MettaParser.MettaParserException.class, () -> parser.parseAtom("(a b) %"));
        assertTrue(exception.getMessage().contains("Trailing characters that could not be tokenized: %"));
    }

    @Test
    void testParseAtom_emptyInput() {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> parser.parseAtom(""));
        assertEquals("Input string cannot be null or empty.", exception.getMessage());
    }

    @Test
    void testParseAtom_blankInput() {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> parser.parseAtom("   "));
        assertEquals("Input string cannot be null or empty.", exception.getMessage());
    }

    @Test
    void testParseScript_emptyInput() {
        List<Atom> atoms = parser.parseScript("");
        assertTrue(atoms.isEmpty());
    }

    @Test
    void testParseScript_blankInput() {
        List<Atom> atoms = parser.parseScript("   ");
        assertTrue(atoms.isEmpty());
    }

    @Test
    void testVariableNamingConvention() {
        // Valid variable names
        assertDoesNotThrow(() -> new VariableAtom("$x"));
        assertDoesNotThrow(() -> new VariableAtom("$X"));
        assertDoesNotThrow(() -> new VariableAtom("$x1"));
        assertDoesNotThrow(() -> new VariableAtom("$x-1"));
        assertDoesNotThrow(() -> new VariableAtom("$foo-bar"));

        // Invalid variable names
        IllegalArgumentException ex1 =
            assertThrows(IllegalArgumentException.class, () -> new VariableAtom("x"));
        assertEquals("Variable name must start with $ and have a non-empty name", ex1.getMessage());

        IllegalArgumentException ex2 =
            assertThrows(IllegalArgumentException.class, () -> new VariableAtom("$"));
        assertEquals("Variable name must start with $ and have a non-empty name", ex2.getMessage());

        IllegalArgumentException ex3 =
            assertThrows(IllegalArgumentException.class, () -> new VariableAtom(null));
        assertEquals("Variable name must start with $ and have a non-empty name", ex3.getMessage());
    }
}
