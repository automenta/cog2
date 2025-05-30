package org.opencog.hyperon.space;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.ExpressionAtom;
import org.opencog.hyperon.atoms.SymbolAtom;
import org.opencog.hyperon.atoms.VariableAtom;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BindingsTest {

    private final VariableAtom varX = new VariableAtom("$X");
    private final VariableAtom varY = new VariableAtom("$Y");
    private final SymbolAtom valA = new SymbolAtom("A");
    private final SymbolAtom valB = new SymbolAtom("B");

    @Test
    void testEmptyBindings() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.getMap().isEmpty());
        assertFalse(bindings.isBound(varX));
        assertNull(bindings.getValue(varX));
    }

    @Test
    void testInitialBindingsConstructor() {
        Map<VariableAtom, Atom> initialMap = new HashMap<>();
        initialMap.put(varX, valA);
        Bindings bindings = new Bindings(initialMap);

        assertTrue(bindings.isBound(varX));
        assertEquals(valA, bindings.getValue(varX));
        assertEquals(1, bindings.getMap().size());
        assertEquals(initialMap, bindings.getMap()); // Compares content
    }

    @Test
    void testInitialBindingsConstructorWithNullMap() {
        Bindings bindings = new Bindings(null);
        assertTrue(bindings.getMap().isEmpty());
    }

    @Test
    void testInitialBindingsConstructorWithNullKeyInMap() {
        Map<VariableAtom, Atom> initialMap = new HashMap<>();
        initialMap.put(null, valA);
        assertThrows(IllegalArgumentException.class, () -> new Bindings(initialMap));
    }

    @Test
    void testInitialBindingsConstructorWithNullValueInMap() {
        Map<VariableAtom, Atom> initialMap = new HashMap<>();
        initialMap.put(varX, null);
        assertThrows(IllegalArgumentException.class, () -> new Bindings(initialMap));
    }


    @Test
    void testAddBinding() {
        Bindings bindings1 = new Bindings();
        Bindings bindings2 = bindings1.addBinding(varX, valA);

        assertFalse(bindings1.isBound(varX)); // Original unchanged
        assertTrue(bindings2.isBound(varX));
        assertEquals(valA, bindings2.getValue(varX));
        assertEquals(1, bindings2.getMap().size());
    }

    @Test
    void testAddMultipleBindings() {
        Bindings bindings = new Bindings()
                .addBinding(varX, valA)
                .addBinding(varY, valB);

        assertTrue(bindings.isBound(varX));
        assertEquals(valA, bindings.getValue(varX));
        assertTrue(bindings.isBound(varY));
        assertEquals(valB, bindings.getValue(varY));
        assertEquals(2, bindings.getMap().size());
    }

    @Test
    void testAddBindingNullVariable() {
        Bindings bindings = new Bindings();
        assertThrows(IllegalArgumentException.class, () -> bindings.addBinding(null, valA));
    }

    @Test
    void testAddBindingNullValue() {
        Bindings bindings = new Bindings();
        assertThrows(IllegalArgumentException.class, () -> bindings.addBinding(varX, null));
    }


    @Test
    void testAddBindingNoConflict() {
        Bindings bindings1 = new Bindings().addBinding(varX, valA);
        Bindings bindings2 = bindings1.addBinding(varX, valA); // Add same binding

        assertSame(bindings1, bindings2, "Adding the same binding should return the same instance.");
        assertEquals(1, bindings2.getMap().size());
    }

    @Test
    void testAddBindingWithConflict() {
        Bindings bindings = new Bindings().addBinding(varX, valA);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bindings.addBinding(varX, valB));
        assertEquals("Variable $X already bound to a different value: A, trying to bind to: B",
                exception.getMessage());
    }

    @Test
    void testSubstituteSymbol() {
        Bindings bindings = new Bindings();
        SymbolAtom sym = new SymbolAtom("S");
        assertEquals(sym, bindings.substitute(sym));
    }

    @Test
    void testSubstituteUnboundVariable() {
        Bindings bindings = new Bindings();
        assertEquals(varX, bindings.substitute(varX));
    }

    @Test
    void testSubstituteBoundVariable() {
        Bindings bindings = new Bindings().addBinding(varX, valA);
        assertEquals(valA, bindings.substitute(varX));
    }

    @Test
    void testSubstituteExpression() {
        VariableAtom varZ = new VariableAtom("$Z");
        SymbolAtom valC = new SymbolAtom("C");
        ExpressionAtom originalExpr = new ExpressionAtom(Arrays.asList(varX, valB, varZ));

        Bindings bindings = new Bindings()
                .addBinding(varX, valA)
                .addBinding(varZ, valC);

        Atom substituted = bindings.substitute(originalExpr);
        assertTrue(substituted instanceof ExpressionAtom);
        ExpressionAtom substitutedExpr = (ExpressionAtom) substituted;

        assertEquals(new ExpressionAtom(Arrays.asList(valA, valB, valC)), substitutedExpr);
    }

    @Test
    void testSubstituteExpressionNoChange() {
        SymbolAtom valC = new SymbolAtom("C");
        ExpressionAtom originalExpr = new ExpressionAtom(Arrays.asList(valA, valB, valC));
        Bindings bindings = new Bindings().addBinding(varX, new SymbolAtom("D")); // varX not in originalExpr

        Atom substituted = bindings.substitute(originalExpr);
        assertSame(originalExpr, substituted, "Should return same instance if no change");
    }


    @Test
    void testSubstituteNestedExpression() {
        ExpressionAtom innerExpr = new ExpressionAtom(Arrays.asList(varY, valA)); // ($Y A)
        ExpressionAtom originalExpr = new ExpressionAtom(Arrays.asList(varX, innerExpr)); // ($X ($Y A))

        SymbolAtom valC = new SymbolAtom("C"); // Declare valC before use
        Bindings bindings = new Bindings()
                .addBinding(varX, valB)  // $X -> B
                .addBinding(varY, valC); // $Y -> C

        ExpressionAtom expectedInner = new ExpressionAtom(Arrays.asList(valC, valA)); // (C A)
        ExpressionAtom expectedOuter = new ExpressionAtom(Arrays.asList(valB, expectedInner)); // (B (C A))

        Atom substituted = bindings.substitute(originalExpr);
        assertEquals(expectedOuter, substituted);
    }

    @Test
    void testEqualsAndHashCode() {
        Bindings bindings1 = new Bindings().addBinding(varX, valA);
        Bindings bindings2 = new Bindings().addBinding(varX, valA);
        Bindings bindings3 = new Bindings().addBinding(varY, valB);
        Bindings bindings4 = new Bindings(Collections.singletonMap(varX, valA));

        assertEquals(bindings1, bindings2);
        assertEquals(bindings1.hashCode(), bindings2.hashCode());

        assertEquals(bindings1, bindings4);
        assertEquals(bindings1.hashCode(), bindings4.hashCode());

        assertNotEquals(bindings1, bindings3);
        assertNotEquals(bindings1.hashCode(), bindings3.hashCode()); // Not strictly required but good practice

        assertNotEquals(bindings1, null);
        assertNotEquals(bindings1, new Object());

        Bindings empty1 = new Bindings();
        Bindings empty2 = new Bindings(new HashMap<>());
        assertEquals(empty1, empty2);
        assertEquals(empty1.hashCode(), empty2.hashCode());
    }

    @Test
    void testToString() {
        Bindings bindings = new Bindings().addBinding(varX, valA).addBinding(varY, valB);
        String str = bindings.toString();
        // Order in HashMap is not guaranteed, so check for content
        assertTrue(str.startsWith("Bindings{"));
        assertTrue(str.contains("$X=A"));
        assertTrue(str.contains("$Y=B"));
        assertTrue(str.endsWith("}"));
    }

    @Test
    void testToStringEmpty() {
        Bindings bindings = new Bindings();
        assertEquals("Bindings{}", bindings.toString());
    }
}
