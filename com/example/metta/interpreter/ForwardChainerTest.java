package com.example.metta.interpreter;

import com.example.metta.atom.*;
import com.example.metta.space.GroundingSpace;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ForwardChainerTest {

    // Helper symbols
    private final SymbolAtom P = new SymbolAtom("P");
    private final SymbolAtom Q = new SymbolAtom("Q");
    private final SymbolAtom R = new SymbolAtom("R");
    private final SymbolAtom S = new SymbolAtom("S");
    private final SymbolAtom T = new SymbolAtom("T");

    private final SymbolAtom valA = new SymbolAtom("a");
    private final SymbolAtom valB = new SymbolAtom("b");
    private final SymbolAtom valC = new SymbolAtom("c");
    private final SymbolAtom valD = new SymbolAtom("d");
    private final SymbolAtom valE = new SymbolAtom("e");

    private final SymbolAtom Q_concrete = new SymbolAtom("Q_concrete");
    private final SymbolAtom R_Not_Q_Concrete = new SymbolAtom("R_Not_Q_Concrete");


    // Tests for OR_SYMBOL
    // Rule: (=> (or (P $x) (Q $x)) (R $x))

    @Test
    void testForwardChainerOrConditionPIsTrue() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        // Rule: (=> (or (P $x) (Q $x)) (R $x))
        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL, new ExpressionAtom(P, x), new ExpressionAtom(Q, x)),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        // Fact: (P a)
        ExpressionAtom factPa = new ExpressionAtom(P, valA);
        // No need to add factPa to space if it's the newFact.
        // If it were an existing fact and another fact triggered, it would be in space.

        Atom newFact = factPa;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valA);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R a) to be derived");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerOrConditionQIsTrue() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL, new ExpressionAtom(P, x), new ExpressionAtom(Q, x)),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factQb = new ExpressionAtom(Q, valB);
        Atom newFact = factQb;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valB);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R b) to be derived");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerOrConditionBothTrueNewFactMatchesP() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL, new ExpressionAtom(P, x), new ExpressionAtom(Q, x)),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPc = new ExpressionAtom(P, valC);
        ExpressionAtom factQc = new ExpressionAtom(Q, valC);
        space.addAtom(factQc); // Q c is an existing fact

        Atom newFact = factPc; // P c is the new fact
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);


        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valC);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R c) to be derived");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerOrConditionBothTrueNewFactMatchesQ() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL, new ExpressionAtom(P, x), new ExpressionAtom(Q, x)),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPd = new ExpressionAtom(P, valD);
        ExpressionAtom factQd = new ExpressionAtom(Q, valD);
        space.addAtom(factPd); // P d is an existing fact

        Atom newFact = factQd; // Q d is the new fact
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valD);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R d) to be derived");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerOrConditionNeitherTrue() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL, new ExpressionAtom(P, x), new ExpressionAtom(Q, x)),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factSe = new ExpressionAtom(S, valE); // A different predicate
        Atom newFact = factSe;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        assertTrue(derivedConclusions.isEmpty(), "Expected no conclusions to be derived");
    }

    @Test
    void testForwardChainerOrWithMultipleVariables() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");
        VariableAtom y = new VariableAtom("y");

        // Rule: (=> (or (P $x $y) (Q $y $x)) (R $x $y))
        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.OR_SYMBOL,
                    new ExpressionAtom(P, x, y),
                    new ExpressionAtom(Q, y, x)),
                new ExpressionAtom(R, x, y)
        );
        space.addAtom(rule);

        ExpressionAtom factPab = new ExpressionAtom(P, valA, valB);
        Atom newFact = factPab;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valA, valB);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R a b) to be derived");
        assertEquals(1, derivedConclusions.size());
    }

    // Tests for NOT_SYMBOL
    // Rule: (=> (and (P $x) (not (Q $x))) (R $x))

    @Test
    void testForwardChainerNotConditionQIsAbsent() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        // Rule: (=> (and (P $x) (not (Q $x))) (R $x))
        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.AND_SYMBOL,
                    new ExpressionAtom(P, x),
                    new ExpressionAtom(MettaSymbols.NOT_SYMBOL, new ExpressionAtom(Q, x))),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPa = new ExpressionAtom(P, valA);
        // Q a is absent
        Atom newFact = factPa;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R, valA);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R a) to be derived when Q a is absent");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerNotConditionQIsPresent() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.AND_SYMBOL,
                    new ExpressionAtom(P, x),
                    new ExpressionAtom(MettaSymbols.NOT_SYMBOL, new ExpressionAtom(Q, x))),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPb = new ExpressionAtom(P, valB);
        ExpressionAtom factQb = new ExpressionAtom(Q, valB); // Q b is present
        space.addAtom(factQb); // Add Q b to existing facts

        Atom newFact = factPb;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        assertTrue(derivedConclusions.isEmpty(), "Expected no conclusions when Q b is present");
    }

    @Test
    void testForwardChainerNotConditionNewFactMakesNotFalse() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.AND_SYMBOL,
                    new ExpressionAtom(P, x),
                    new ExpressionAtom(MettaSymbols.NOT_SYMBOL, new ExpressionAtom(Q, x))),
                new ExpressionAtom(R, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPc = new ExpressionAtom(P, valC);
        space.addAtom(factPc); // P c is an existing fact

        // This is the specific scenario: newFact is (Q c), which makes (not (Q c)) false.
        // The rule is triggered by (P c) which is already in existingFactsWithNew.
        // This means the trigger method needs to be able to be called even if newFact is already "known".
        // Or, more precisely, newFact is the fact that initiates the chaining, and it must match a positive conjunct.
        Atom newFactIsPc = factPc; // Let (P c) be the trigger

        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        // existingFactsWithNew already contains (P c).
        // Now, add (Q c) to the knowledge base *before* triggering with (P c).
        ExpressionAtom factQc = new ExpressionAtom(Q, valC);
        existingFactsWithNew.add(factQc); // (Q c) is now part of the facts to check against for (not (Q c))

        Set<Atom> derivedConclusions = chainer.trigger(space, newFactIsPc, existingFactsWithNew);

        assertTrue(derivedConclusions.isEmpty(), "Expected (R c) NOT derived because (Q c) was added to existing facts, making (not (Q c)) false.");
    }


    @Test
    void testForwardChainerNotWithSpecificGroundAtomQIsNotPresent() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        // Rule: (=> (and (P $x) (not Q_concrete)) (R_Not_Q_Concrete $x))
        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.AND_SYMBOL,
                    new ExpressionAtom(P, x),
                    new ExpressionAtom(MettaSymbols.NOT_SYMBOL, Q_concrete)), // Q_concrete is a SymbolAtom
                new ExpressionAtom(R_Not_Q_Concrete, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPa = new ExpressionAtom(P, valA);
        // Q_concrete is NOT in space
        Atom newFact = factPa;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        ExpressionAtom expectedConclusion = new ExpressionAtom(R_Not_Q_Concrete, valA);
        assertTrue(derivedConclusions.contains(expectedConclusion), "Expected (R_Not_Q_Concrete a) when Q_concrete absent");
        assertEquals(1, derivedConclusions.size());
    }

    @Test
    void testForwardChainerNotWithSpecificGroundAtomQIsPresent() {
        GroundingSpace space = new GroundingSpace();
        ForwardChainer chainer = new ForwardChainer();
        VariableAtom x = new VariableAtom("x");

        ExpressionAtom rule = new ExpressionAtom(
                MettaSymbols.IMPLIES_SYMBOL,
                new ExpressionAtom(MettaSymbols.AND_SYMBOL,
                    new ExpressionAtom(P, x),
                    new ExpressionAtom(MettaSymbols.NOT_SYMBOL, Q_concrete)),
                new ExpressionAtom(R_Not_Q_Concrete, x)
        );
        space.addAtom(rule);

        ExpressionAtom factPb = new ExpressionAtom(P, valB);
        space.addAtom(Q_concrete); // Q_concrete IS in space

        Atom newFact = factPb;
        Set<Atom> existingFactsWithNew = new HashSet<>(space.getAtoms());
        existingFactsWithNew.add(newFact);

        Set<Atom> derivedConclusions = chainer.trigger(space, newFact, existingFactsWithNew);

        assertTrue(derivedConclusions.isEmpty(), "Expected no conclusion when Q_concrete is present");
    }
}
