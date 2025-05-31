package com.example.metta.interpreter;

import com.example.metta.atom.*;
import com.example.metta.space.GroundingSpace;
import com.example.metta.text.SExprParser;
import com.example.metta.types.Bindings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BackwardChainerTest {

    private GroundingSpace space;
    private SExprParser parser;

    @BeforeEach
    void setUp() {
        space = new GroundingSpace();
        // Initialize parser with an empty string or a default source name if needed
        parser = new SExprParser("");
    }

    private void add(String expression) {
        space.addAtom(parser.parse(expression));
    }

    // Helper to find if a specific variable binding exists in a list of Bindings
    private boolean findBinding(List<Bindings> bindingsList, VariableAtom var, Atom expectedValue) {
        return bindingsList.stream().anyMatch(b -> {
            Atom resolvedValue = b.resolve(var);
            return expectedValue.equals(resolvedValue);
        });
    }

    // Helper to find if a specific variable binding (varName as String) exists
    private boolean findBinding(List<Bindings> bindingsList, String varName, Atom expectedValue) {
        return findBinding(bindingsList, new VariableAtom(varName), expectedValue);
    }


    // 1. Basic Fact Proving
    @Test
    void testProveFactExists() {
        add("(FactA)");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(FactA)"));
        assertFalse(result.isEmpty(), "Should prove existing fact.");
        // For a ground fact, it should return one empty binding set
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    void testProveFactDoesNotExist() {
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(NonExistentFact)"));
        assertTrue(result.isEmpty(), "Should not prove non-existent fact.");
    }

    @Test
    void testProveFactWithVariableMatch() {
        add("(P a)");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        VariableAtom x = new VariableAtom("$x");
        List<Bindings> result = chainer.prove(parser.parse("(P $x)"));
        assertEquals(1, result.size(), "Should find one match.");
        assertEquals(new SymbolAtom("a"), result.get(0).resolve(x));
    }

    // 2. Simple Rule Proving
    @Test
    void testProveSimpleRuleNoVariables() {
        add("(CondA)");
        add("(=> (CondA) (ConcA))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(ConcA)"));
        assertFalse(result.isEmpty(), "Should prove conclusion via simple rule.");
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    void testProveSimpleRuleWithVariablePropagation() {
        add("(CondB b)");
        add("(=> (CondB $x) (ConcB $x))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        VariableAtom y = new VariableAtom("$y");
        List<Bindings> result = chainer.prove(parser.parse("(ConcB $y)"));
        assertEquals(1, result.size(), "Should propagate variable binding.");
        assertEquals(new SymbolAtom("b"), result.get(0).resolve(y));
    }

    @Test
    void testProveRuleConditionFalse() {
        add("(=> (CondC_NonExist) (ConcC))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(ConcC)"));
        assertTrue(result.isEmpty(), "Should not prove rule if condition is false.");
    }

    // 3. Recursive Rule Proving
    @Test
    void testProveRecursiveRule() {
        add("(BaseZ)");
        add("(=> (BaseZ) (MidZ))");
        add("(=> (MidZ) (FinalZ))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(FinalZ)"));
        assertFalse(result.isEmpty(), "Should prove through recursive rule application.");
        assertEquals(1, result.size());
    }

    // 4. Logical Connective AND
    @Test
    void testProveAndBothTrue() {
        add("(P1)");
        add("(P2)");
        add("(=> (and (P1) (P2)) (R_And))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_And)"));
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testProveAndOneFalse() {
        add("(P1)");
        // (P_NonExist) is not added
        add("(=> (and (P1) (P_NonExist)) (R_And_Fail))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_And_Fail)"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testProveAndWithVariables() {
        add("(Person john)");
        add("(Likes john chocolate)");
        add("(=> (and (Person $x) (Likes $x chocolate)) (ChocolateLiker $x))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(ChocolateLiker $y)"));
        assertEquals(1, result.size());
        assertEquals(new SymbolAtom("john"), result.get(0).resolve(new VariableAtom("$y")));
    }

    // 5. Logical Connective OR
    @Test
    void testProveOrFirstTrue() {
        add("(P1_OR)");
        add("(=> (or (P1_OR) (P2_OR_NonExist)) (R_OR))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_OR)"));
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testProveOrSecondTrue() {
        add("(P2_OR)");
        add("(=> (or (P1_OR_NonExist) (P2_OR)) (R_OR_2))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_OR_2)"));
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testProveOrBothFalse() {
        add("(=> (or (P1_OR_NonExist) (P2_OR_NonExist)) (R_OR_Fail))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_OR_Fail)"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testProveOrWithVariables() {
        add("(Parent mary john)");
        add("(Parent david lisa)");
        add("(=> (or (Parent $p john) (Parent $p lisa)) (ParentOfJohnOrLisa $p))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> results = chainer.prove(parser.parse("(ParentOfJohnOrLisa $x)"));
        assertEquals(2, results.size(), "Expected two parents: mary and david.");
        assertTrue(findBinding(results, "$x", new SymbolAtom("mary")));
        assertTrue(findBinding(results, "$x", new SymbolAtom("david")));
    }

    // 6. Logical Connective NOT
    @Test
    void testProveNotArgFalse() {
        // (Q_Not_NonExist) is not in the space, so (not (Q_Not_NonExist)) is true.
        add("(=> (not (Q_Not_NonExist)) (R_Not_Q_Absent))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_Not_Q_Absent)"));
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testProveNotArgTrue() {
        add("(Q_Not_Exist)");
        add("(=> (not (Q_Not_Exist)) (R_Not_Q_Present))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> result = chainer.prove(parser.parse("(R_Not_Q_Present)"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testProveNotWithVariables() {
        add("(Student fred)");
        // No (IsLazy fred) fact initially
        add("(=> (and (Student $x) (not (IsLazy $x))) (HardWorkingStudent $x))");
        BackwardChainer chainer = new BackwardChainer(space, 5);

        // Test 1: (IsLazy fred) is not in space
        List<Bindings> result1 = chainer.prove(parser.parse("(HardWorkingStudent fred)"));
        assertFalse(result1.isEmpty(), "Fred should be hardworking if not lazy.");
        assertEquals(new SymbolAtom("fred"), result1.get(0).resolve(new VariableAtom("$x"))); // from (Student fred)

        // Test 2: Add (IsLazy fred) and re-prove
        add("(IsLazy fred)"); // Now fred is lazy
        List<Bindings> result2 = chainer.prove(parser.parse("(HardWorkingStudent fred)"));
        assertTrue(result2.isEmpty(), "Fred should not be hardworking if lazy.");
    }

    // 7. Recursion Depth Limit
    @Test
    void testRecursionDepthLimitReached() {
        add("(=> (Loop $x) (Loop $x))"); // Rule that can cause infinite recursion
        add("(Loop start)"); // Fact to initiate
        // Query for (Loop end) which can only be proven by the recursive rule.
        // If (Loop $x) was the query, it would match (Loop start) at depth 0.
        // Querying for something not directly a fact forces rule use.
        BackwardChainer chainer = new BackwardChainer(space, 3); // Max depth 3
        List<Bindings> result = chainer.prove(parser.parse("(Loop end)"));
        assertTrue(result.isEmpty(), "Should fail to prove due to depth limit, not loop forever.");
    }

    @Test
    void testRecursionDepthSufficientAndInsufficient() {
        add("(A X)");
        add("(=> (A $x) (B $x))"); // Depth 1 for (B $x) from (A $x)
        add("(=> (B $x) (C $x))"); // Depth 1 for (C $x) from (B $x)
                                  // Total depth to prove (C X) from (A X) is 2 rule applications.

        // Query: (C X)
        // proveRecursive( (C X), bindings, 0)
        //   rule (=> (B $x) (C $x)), unifies $x=X with (C X)
        //   proveRecursive( (B X), bindings', 1)
        //     rule (=> (A $y) (B $y)), unifies $y=X with (B X)
        //     proveRecursive( (A X), bindings'', 2)
        //       fact (A X) matches, returns bindings
        //     returns to depth 1 call
        //   returns to depth 0 call
        // Result: success

        BackwardChainer chainerSufficient = new BackwardChainer(space, 2); // Max depth for rule applications
        List<Bindings> resultSufficient = chainerSufficient.prove(parser.parse("(C X)"));
        assertFalse(resultSufficient.isEmpty(), "Should prove (C X) with max depth 2.");
        assertEquals(1, resultSufficient.size());

        BackwardChainer chainerInsufficient = new BackwardChainer(space, 1); // Max depth 1
        List<Bindings> resultInsufficient = chainerInsufficient.prove(parser.parse("(C X)"));
        assertTrue(resultInsufficient.isEmpty(), "Should fail to prove (C X) with max depth 1.");
    }

    // 8. Multiple Rule Paths / Multiple Results
    @Test
    void testMultipleResultsForQuery() {
        add("(P a)");
        add("(P b)");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> results = chainer.prove(parser.parse("(P $x)"));
        assertEquals(2, results.size(), "Should find two results for (P $x).");
        assertTrue(findBinding(results, "$x", new SymbolAtom("a")));
        assertTrue(findBinding(results, "$x", new SymbolAtom("b")));
    }

    @Test
    void testMultipleRulesForSameConclusion() {
        add("(Cond1_Multi)");
        add("(Cond2_Multi)");
        add("(=> (Cond1_Multi) (ConcMulti))");
        add("(=> (Cond2_Multi) (ConcMulti))");
        BackwardChainer chainer = new BackwardChainer(space, 5);
        List<Bindings> results = chainer.prove(parser.parse("(ConcMulti)"));
        // Each rule path provides one way to prove (ConcMulti), resulting in an empty binding.
        // Depending on how Bindings equality and list processing are handled,
        // this might be 1 or 2. If Bindings are unique objects even if empty, it's 2.
        // If they are considered equal and filtered by a Set intermediate, it might be 1.
        // The current BackwardChainer implementation adds all results from sub-proofs directly.
        assertEquals(2, results.size(), "Should find two proof paths for (ConcMulti).");
    }
}
