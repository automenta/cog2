package org.opencog.hyperon.space;

import org.opencog.hyperon.atoms.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AtomSpaceTest {

    private AtomSpace space;
    private final SymbolAtom symA = new SymbolAtom("A");
    private final SymbolAtom symB = new SymbolAtom("B");
    private final SymbolAtom symC = new SymbolAtom("C");
    private final VariableAtom varX = new VariableAtom("$X");
    private final VariableAtom varY = new VariableAtom("$Y");

    @BeforeEach
    void setUp() {
        space = new AtomSpace();
    }

    @Test
    void testAddAtom() {
        space.addAtom(symA);
        assertTrue(space.getAtoms().contains(symA));
        assertEquals(1, space.getAtoms().size());
    }

    @Test
    void testAddMultipleAtoms() {
        ExpressionAtom exprAB = new ExpressionAtom(Arrays.asList(symA, symB));
        space.addAtom(symA);
        space.addAtom(exprAB);
        assertEquals(2, space.getAtoms().size());
        assertTrue(space.getAtoms().contains(symA));
        assertTrue(space.getAtoms().contains(exprAB));
    }

    @Test
    void testAddAtomNull() {
        assertThrows(IllegalArgumentException.class, () -> space.addAtom(null));
    }

    @Test
    void testAddExpressionWithNullChild() {
        ExpressionAtom exprWithNull = new ExpressionAtom(Arrays.asList(symA, null));
        assertThrows(IllegalArgumentException.class, () -> space.addAtom(exprWithNull));
    }


    @Test
    void testGetAtomsReturnsUnmodifiableSet() {
        space.addAtom(symA);
        Set<Atom> atoms = space.getAtoms();
        assertThrows(UnsupportedOperationException.class, () -> atoms.add(symB));
    }

    @Test
    void testQueryForConcreteSymbol() {
        space.addAtom(symA);
        space.addAtom(symB);
        List<Bindings> result = space.query(symA);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getMap().isEmpty()); // No variables, empty bindings
    }

    @Test
    void testQueryForNonExistentSymbol() {
        space.addAtom(symA);
        List<Bindings> result = space.query(symB);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryForConcreteExpression() {
        ExpressionAtom exprAB = new ExpressionAtom(Arrays.asList(symA, symB));
        ExpressionAtom exprAC = new ExpressionAtom(Arrays.asList(symA, symC));
        space.addAtom(exprAB);
        space.addAtom(exprAC);

        List<Bindings> result = space.query(exprAB);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getMap().isEmpty());
    }

    @Test
    void testQueryForExpressionNoMatchDifferentSize() {
        ExpressionAtom exprAB = new ExpressionAtom(Arrays.asList(symA, symB));
        space.addAtom(exprAB);

        ExpressionAtom patternABC = new ExpressionAtom(Arrays.asList(symA, symB, symC));
        List<Bindings> result = space.query(patternABC);
        assertTrue(result.isEmpty());
    }


    @Test
    void testQueryWithVariableMatchingSymbol() {
        space.addAtom(symA);
        space.addAtom(symB);

        List<Bindings> result = space.query(varX); // Query with just a variable
        assertEquals(2, result.size());

        boolean foundBindingForA = false;
        boolean foundBindingForB = false;
        for (Bindings b : result) {
            if (b.getValue(varX).equals(symA)) {
                foundBindingForA = true;
            }
            if (b.getValue(varX).equals(symB)) {
                foundBindingForB = true;
            }
        }
        assertTrue(foundBindingForA, "Should find binding for $X=A");
        assertTrue(foundBindingForB, "Should find binding for $X=B");
    }

    @Test
    void testQueryExprWithOneVariable() {
        // Space: (A B), (A C)
        // Query: ($X B)
        ExpressionAtom dataAB = new ExpressionAtom(Arrays.asList(symA, symB));
        ExpressionAtom dataAC = new ExpressionAtom(Arrays.asList(symA, symC));
        space.addAtom(dataAB);
        space.addAtom(dataAC);

        ExpressionAtom patternXB = new ExpressionAtom(Arrays.asList(varX, symB));
        List<Bindings> result = space.query(patternXB);

        assertEquals(1, result.size());
        Bindings bindings = result.get(0);
        assertEquals(symA, bindings.getValue(varX));
    }

    @Test
    void testQueryExprWithMultipleVariables() {
        // Space: (A B), (C D)
        // Query: ($X $Y)
        ExpressionAtom dataAB = new ExpressionAtom(Arrays.asList(symA, symB));
        ExpressionAtom dataCD = new ExpressionAtom(Arrays.asList(symC, new SymbolAtom("D")));
        space.addAtom(dataAB);
        space.addAtom(dataCD);
        space.addAtom(symA); // Should not match ($X $Y)

        ExpressionAtom patternXY = new ExpressionAtom(Arrays.asList(varX, varY));
        List<Bindings> result = space.query(patternXY);

        assertEquals(2, result.size());
        boolean foundAB = false;
        boolean foundCD = false;
        for (Bindings b : result) {
            if (b.getValue(varX).equals(symA) && b.getValue(varY).equals(symB)) {
                foundAB = true;
            } else if (b.getValue(varX).equals(symC) && b.getValue(varY).equals(new SymbolAtom("D"))) {
                foundCD = true;
            }
        }
        assertTrue(foundAB, "Did not find binding for ($X=A, $Y=B)");
        assertTrue(foundCD, "Did not find binding for ($X=C, $Y=D)");
    }

    @Test
    void testQueryVariableBoundToDifferentValuesInPattern() {
        // Pattern ($X $X), Data (A A) -> match, $X = A
        // Pattern ($X $X), Data (A B) -> no match
        ExpressionAtom dataAA = new ExpressionAtom(Arrays.asList(symA, symA));
        ExpressionAtom dataAB = new ExpressionAtom(Arrays.asList(symA, symB));
        space.addAtom(dataAA);
        space.addAtom(dataAB);

        ExpressionAtom patternXX = new ExpressionAtom(Arrays.asList(varX, varX));
        List<Bindings> result = space.query(patternXX);

        assertEquals(1, result.size());
        assertEquals(symA, result.get(0).getValue(varX));
    }

    @Test
    void testQueryNestedExpressionWithVariables() {
        // Space: (Equals (Plus $N (S Z)) $N)
        //        (Equals (Plus One (S Zero)) One)
        // Query: (Equals (Plus $A $B) $A)
        // Expected: $A = One, $B = (S Zero)

        SymbolAtom eq = new SymbolAtom("Equals");
        SymbolAtom plus = new SymbolAtom("Plus");
        SymbolAtom s = new SymbolAtom("S");
        SymbolAtom z = new SymbolAtom("Zero");
        SymbolAtom one = new SymbolAtom("One");

        // Data: (Equals (Plus One (S Zero)) One)
        ExpressionAtom sZero = new ExpressionAtom(Arrays.asList(s, z)); // (S Zero)
        ExpressionAtom plusOneSZero = new ExpressionAtom(Arrays.asList(plus, one, sZero)); // (Plus One (S Zero))
        ExpressionAtom dataAtom = new ExpressionAtom(Arrays.asList(eq, plusOneSZero, one)); // (Equals (Plus One (S Zero)) One)
        space.addAtom(dataAtom);

        // Pattern: (Equals (Plus $A $B) $A)
        VariableAtom varA = new VariableAtom("$A");
        VariableAtom varB = new VariableAtom("$B");
        ExpressionAtom plusAB = new ExpressionAtom(Arrays.asList(plus, varA, varB)); // (Plus $A $B)
        ExpressionAtom patternAtom = new ExpressionAtom(Arrays.asList(eq, plusAB, varA)); // (Equals (Plus $A $B) $A)

        List<Bindings> result = space.query(patternAtom);
        assertEquals(1, result.size(), "Should find one matching binding set.");

        Bindings bindings = result.get(0);
        assertEquals(one, bindings.getValue(varA));
        assertEquals(sZero, bindings.getValue(varB));
    }

    @Test
    void testQueryWithGroundedAtom() {
        Groundable op = args -> new SymbolAtom("executed");
        GroundedAtom ga = new GroundedAtom("MyOp", op);
        space.addAtom(ga);

        List<Bindings> resultExact = space.query(new GroundedAtom("MyOp", op));
        assertEquals(1, resultExact.size());

        List<Bindings> resultVar = space.query(varX);
        boolean found = false;
        for(Bindings b : resultVar) {
            if (b.getValue(varX).equals(ga)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void testQueryExpressionWithGroundedAtom() {
        Groundable op = args -> new SymbolAtom("executed");
        GroundedAtom ga = new GroundedAtom("MyOp", op);
        ExpressionAtom dataExpr = new ExpressionAtom(Arrays.asList(symA, ga));
        space.addAtom(dataExpr);

        // Exact match
        List<Bindings> resultExact = space.query(new ExpressionAtom(Arrays.asList(symA, new GroundedAtom("MyOp", op))));
        assertEquals(1, resultExact.size());

        // Match with variable for GroundedAtom
        ExpressionAtom patternVar = new ExpressionAtom(Arrays.asList(symA, varX));
        List<Bindings> resultVar = space.query(patternVar);
        assertEquals(1, resultVar.size());
        assertEquals(ga, resultVar.get(0).getValue(varX));
    }
}
