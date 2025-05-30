package com.example.metta.matcher;

import com.example.metta.atom.*;
import com.example.metta.types.Bindings;
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience parsing

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Collections;

public class MatcherTest {

    private final VariableAtom x = new VariableAtom("x");
    private final VariableAtom y = new VariableAtom("y");
    private final SymbolAtom a = new SymbolAtom("A");
    private final SymbolAtom b = new SymbolAtom("B");
    private final SymbolAtom f = new SymbolAtom("f");
    private final SymbolAtom g = new SymbolAtom("g");
    private final SymbolAtom h = new SymbolAtom("h");

    @Test
    void symbolVsSymbol() {
        List<Bindings> result = Matcher.matchAtoms(a, a);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());

        result = Matcher.matchAtoms(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    void variableVsSymbol() {
        List<Bindings> result = Matcher.matchAtoms(x, a);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isEmpty());
        assertEquals(a, result.get(0).resolve(x));
    }

    @Test
    void symbolVsVariable() {
        List<Bindings> result = Matcher.matchAtoms(a, x);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isEmpty());
        assertEquals(a, result.get(0).resolve(x));
    }

    @Test
    void variableVsVariable() {
        List<Bindings> result = Matcher.matchAtoms(x, y);
        assertEquals(1, result.size());
        Bindings b = result.get(0);
        // x should be bound to y (or y to x)
        Atom resolvedX = b.resolve(x);
        Atom resolvedY = b.resolve(y);
        assertTrue(resolvedX.equals(y) || resolvedY.equals(x) || resolvedX.equals(resolvedY)); // x and y are in the same set
                                                                          // e.g. x -> y or y -> x
        // To confirm, bind one and check the other
        b.addValueBinding(y, a);
        assertEquals(a, b.resolve(x));
    }
    
    @Test
    void variableVsVariableAlreadyBound() {
        Bindings initialBindings = new Bindings();
        initialBindings.addValueBinding(x, a);

        // Matcher.matchAtoms starts with its own new Bindings.
        // To test with pre-existing bindings, those bindings need to be part of the atoms being matched,
        // or the Matcher needs to accept initial bindings. The current Matcher API doesn't directly support this.
        // However, the recursive calls within Matcher *do* pass bindings.
        // The test here is for matchAtoms(Atom, Atom) which starts fresh.

        // Scenario: (f $x) vs (f $y), where $x is A.
        // This would be tested by matching expressions.
        // If we match $x (bound to A in some context) vs $y:
        // This test is more about how existing bindings (if Matcher were to take them) interact.
        // Let's test this via expression matching.
        
        ExpressionAtom pattern = new ExpressionAtom(List.of(f, x)); // (f $x)
        ExpressionAtom target  = new ExpressionAtom(List.of(f, y)); // (f $y)

        // Simulate $x being bound to A beforehand by applying it to the pattern
        // This is how one might use it if bindings were external.
        // Atom concretePattern = Matcher.applyBindings(pattern, initialBindings); // (f A)
        // List<Bindings> result = Matcher.matchAtoms(concretePattern, target);

        // Instead, let's test the scenario where a variable within the pattern is matched multiple times
        // e.g. ($x $x) vs (A A)
        ExpressionAtom patternSameVar = new ExpressionAtom(List.of(x,x));
        ExpressionAtom targetAA = new ExpressionAtom(List.of(a,a));
        List<Bindings> resultSameVar = Matcher.matchAtoms(patternSameVar, targetAA);
        assertEquals(1, resultSameVar.size());
        assertEquals(a, resultSameVar.get(0).resolve(x));
        
        ExpressionAtom targetAB = new ExpressionAtom(List.of(a,b));
        resultSameVar = Matcher.matchAtoms(patternSameVar, targetAB);
        assertTrue(resultSameVar.isEmpty()); // $x cannot be A and B
    }


    @Test
    void expressionVsExpression() {
        ExpressionAtom pattern = new ExpressionAtom(List.of(f, x, b)); // (f $x B)
        ExpressionAtom target  = new ExpressionAtom(List.of(f, a, b)); // (f A B)
        List<Bindings> result = Matcher.matchAtoms(pattern, target);
        assertEquals(1, result.size());
        assertEquals(a, result.get(0).resolve(x));

        // Mismatch length
        ExpressionAtom targetShort = new ExpressionAtom(List.of(f, a));
        result = Matcher.matchAtoms(pattern, targetShort);
        assertTrue(result.isEmpty());

        // Mismatch operator
        ExpressionAtom targetWrongOp = new ExpressionAtom(List.of(g, a, b));
        result = Matcher.matchAtoms(pattern, targetWrongOp);
        assertTrue(result.isEmpty());

        // Mismatch concrete part
        ExpressionAtom targetWrongConst = new ExpressionAtom(List.of(f, a, a)); // Expected (f $x B)
        result = Matcher.matchAtoms(pattern, targetWrongConst);
        assertTrue(result.isEmpty());
    }

    @Test
    void nestedExpression() {
        // Pattern: (f ($g A) $y)  Target: (f (h A) B)
        // Expected: $g=h, $y=B
        VariableAtom gVar = new VariableAtom("g");
        ExpressionAtom pattern = new ExpressionAtom(List.of(f, new ExpressionAtom(List.of(gVar, a)), y));
        ExpressionAtom target  = new ExpressionAtom(List.of(f, new ExpressionAtom(List.of(this.h, a)), b));
        // SymbolAtom h_sym = new SymbolAtom("h"); // Removed local variable

        List<Bindings> result = Matcher.matchAtoms(pattern, target);
        assertEquals(1, result.size());
        Bindings bnd = result.get(0);
        assertEquals(this.h, bnd.resolve(gVar)); // Use field this.h
        assertEquals(b, bnd.resolve(y));
    }

    @Test
    void groundedAtomVsGroundedAtom() {
        GroundedAtom ga1 = new GroundedAtom(42);
        GroundedAtom ga2 = new GroundedAtom(42);
        GroundedAtom ga3 = new GroundedAtom(100);

        List<Bindings> result = Matcher.matchAtoms(ga1, ga2); // Should match by equality
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());

        result = Matcher.matchAtoms(ga1, ga3);
        assertTrue(result.isEmpty());
    }

    @Test
    void variableVsGroundedAtom() {
        GroundedAtom ga = new GroundedAtom(42);
        List<Bindings> result = Matcher.matchAtoms(x, ga);
        assertEquals(1, result.size());
        assertEquals(ga, result.get(0).resolve(x));
    }
    
    @Test
    void expressionWithGroundedAtom() {
        // Pattern: (data $x) Target: (data {42})
        GroundedAtom ga = new GroundedAtom(42);
        SymbolAtom dataSym = new SymbolAtom("data");
        ExpressionAtom pattern = new ExpressionAtom(List.of(dataSym, x));
        ExpressionAtom target  = new ExpressionAtom(List.of(dataSym, ga));
        
        List<Bindings> result = Matcher.matchAtoms(pattern, target);
        assertEquals(1, result.size());
        assertEquals(ga, result.get(0).resolve(x));
    }

    // Test for Groundable.match() interaction - requires a custom Groundable
    private static class CustomMatchGroundable implements Groundable {
        private final Atom patternToMatch; // What this Groundable atom represents as a pattern
        private final List<Bindings> matchResultsToReturn; // Predefined results it will return

        CustomMatchGroundable(Atom patternToMatch, List<Bindings> matchResultsToReturn) {
            this.patternToMatch = patternToMatch;
            this.matchResultsToReturn = matchResultsToReturn;
        }

        @Override public Atom getType() { return new SymbolAtom("CustomMatcher"); }
        @Override public List<Atom> execute(List<Atom> args) { throw new UnsupportedOperationException(); }
        @Override public String toDisplayString() { return "CustomMatcher{" + patternToMatch + "}"; }

        @Override
        public List<Bindings> match(Atom other) {
            // This custom match logic only "succeeds" if 'other' is equal to its 'patternToMatch'
            // and then returns its predefined bindings.
            if (patternToMatch.equals(other)) { // Simple equality check for the demo
                return matchResultsToReturn;
            }
            return java.util.Collections.emptyList(); // Fully qualified because import is at top-level of MatcherTest.java
        }
    }

    @Test
    void groundedAtomWithCustomMatch() {
        // Pattern is a GroundedAtom with custom match logic
        Bindings customBinding = new Bindings();
        customBinding.addValueBinding(x, b); // $x = B
        CustomMatchGroundable cmg = new CustomMatchGroundable(a, java.util.Collections.singletonList(customBinding)); // This will match 'A' and return $x=B
        GroundedAtom patternGrounded = new GroundedAtom(cmg); // Wrap the Groundable logic

        // Target is 'A'
        List<Bindings> result = Matcher.matchAtoms(patternGrounded, a);
        assertEquals(1, result.size());
        assertEquals(b, result.get(0).resolve(x)); // Check if $x=B was returned

        // Target is 'B' (should not match according to CustomMatchGroundable logic)
        result = Matcher.matchAtoms(patternGrounded, b);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void targetGroundedAtomWithCustomMatch() {
        // Target is a GroundedAtom with custom match logic
        Bindings customBinding = new Bindings();
        customBinding.addValueBinding(y, a); // $y = A
        CustomMatchGroundable cmgTarget = new CustomMatchGroundable(b, java.util.Collections.singletonList(customBinding)); // This will match 'B' and return $y=A
        GroundedAtom targetGrounded = new GroundedAtom(cmgTarget);

        // Pattern is 'B'
        List<Bindings> result = Matcher.matchAtoms(b, targetGrounded); // Matcher calls target.match(pattern)
        assertEquals(1, result.size());
        assertEquals(a, result.get(0).resolve(y));

        // Pattern is 'A' (should not match)
        result = Matcher.matchAtoms(a, targetGrounded);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void matchWithEmptyExpression() {
        ExpressionAtom emptyExpr = new ExpressionAtom(java.util.Collections.emptyList());
        List<Bindings> result = Matcher.matchAtoms(emptyExpr, emptyExpr);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());

        result = Matcher.matchAtoms(emptyExpr, atom("(a)"));
        assertTrue(result.isEmpty());
        
        result = Matcher.matchAtoms(x, emptyExpr);
        assertEquals(1, result.size());
        assertEquals(emptyExpr, result.get(0).resolve(x));
    }
}
