package com.example.metta.interpreter;

import com.example.metta.atom.Atom;
import com.example.metta.atom.SymbolAtom;
import com.example.metta.atom.ExpressionAtom;
import com.example.metta.atom.MettaSymbols; // For IMPLIES_SYMBOL, AND_SYMBOL
import com.example.metta.space.GroundingSpace;
import com.example.metta.text.SExprParser; // To parse textual representations of atoms

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

public class ForwardChainerTest {

    private Atom parse(String mettaSrc) {
        SExprParser parser = new SExprParser();
        return parser.parse(mettaSrc);
    }

    @Test
    void testSimpleAncestorRule() {
        GroundingSpace space = new GroundingSpace();
        space.setEnableForwardChaining(true);

        // Define rule: (=> (parent $x $y) (ancestor $x $y))
        Atom rule1 = parse("(=> (parent $x $y) (ancestor $x $y))");
        space.add(rule1);

        // Add fact: (parent a b)
        Atom fact1 = parse("(parent a b)");
        space.add(fact1);

        // Expected derived fact: (ancestor a b)
        Atom expectedConclusion1 = parse("(ancestor a b)");
        assertTrue(space.getAtoms().contains(expectedConclusion1), "Should derive (ancestor a b)");
    }

    @Test
    void testTransitiveAncestorRule() {
        GroundingSpace space = new GroundingSpace();
        space.setEnableForwardChaining(true);

        // Define rules
        Atom rule1 = parse("(=> (parent $x $y) (ancestor $x $y))"); // (parent $x $y) -> (ancestor $x $y)
        Atom rule2 = parse("(=> (And (parent $x $y) (ancestor $y $z)) (ancestor $x $z))"); // (parent $x $y) & (ancestor $y $z) -> (ancestor $x $z)

        space.add(rule1);
        space.add(rule2);

        // Add facts
        Atom factParentAB = parse("(parent a b)");
        Atom factParentBC = parse("(parent b c)");

        space.add(factParentAB); // Should derive (ancestor a b) via rule1
        space.add(factParentBC); // Should derive (ancestor b c) via rule1
                                 // AND then (ancestor a c) via rule2 using (parent a b) and newly derived (ancestor b c)

        // Check for all expected conclusions
        Atom ancestorAB = parse("(ancestor a b)");
        Atom ancestorBC = parse("(ancestor b c)");
        Atom ancestorAC = parse("(ancestor a c)");

        Set<Atom> currentAtoms = Set.copyOf(space.getAtoms()); // Use a copy for stable check

        assertTrue(currentAtoms.contains(ancestorAB), "Missing (ancestor a b)");
        assertTrue(currentAtoms.contains(ancestorBC), "Missing (ancestor b c)");
        assertTrue(currentAtoms.contains(ancestorAC), "Missing transitive conclusion (ancestor a c)");
    }

    @Test
    void testNoInfiniteLoopOnExistingFact() {
        GroundingSpace space = new GroundingSpace();
        space.setEnableForwardChaining(true);

        Atom rule = parse("(=> (status $x active) (status $x processed))");
        space.add(rule);

        Atom fact1 = parse("(status Alice active)");
        space.add(fact1); // Derives (status Alice processed)

        assertTrue(space.getAtoms().contains(parse("(status Alice processed)")), "Initial derivation failed.");

        int initialSize = space.getAtoms().size();

        // Add the same fact again, should not cause re-derivation or loop
        space.add(fact1);
        assertEquals(initialSize, space.getAtoms().size(), "Adding existing fact changed atom count.");

        // Add a derived fact again
        Atom derivedFact = parse("(status Alice processed)");
        space.add(derivedFact);
        assertEquals(initialSize, space.getAtoms().size(), "Adding existing derived fact changed atom count.");
    }

    @Test
    void testChainingOrderAndMultiplePaths() {
        GroundingSpace space = new GroundingSpace();
        space.setEnableForwardChaining(true);

        // Rules
        space.add(parse("(=> (A $x) (B $x))")); // A(x) -> B(x)
        space.add(parse("(=> (B $x) (C $x))")); // B(x) -> C(x)
        space.add(parse("(=> (A $x) (D $x))")); // A(x) -> D(x)

        // Fact
        space.add(parse("(A data)"));

        // Expected
        assertTrue(space.getAtoms().contains(parse("(A data)")), "Fact A not present");
        assertTrue(space.getAtoms().contains(parse("(B data)")), "Fact B not derived");
        assertTrue(space.getAtoms().contains(parse("(C data)")), "Fact C not derived (from B)");
        assertTrue(space.getAtoms().contains(parse("(D data)")), "Fact D not derived (from A)");
    }
}
