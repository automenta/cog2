package com.example.metta.interpreter;

import com.example.metta.atom.Atom;
import com.example.metta.atom.ExpressionAtom; // Explicit import
import com.example.metta.space.GroundingSpace;
import static com.example.metta.testing.MettaTestUtils.*; // atom(), interpret(), createSpace(), interpretToStr()

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Arrays; // For List.of in older Java versions if needed, but direct List.of is fine for Java 9+

public class InterpreterExamplesTest {

    @Test
    void selfEvaluatingAtoms() {
        GroundingSpace space = createSpace("");
        
        // (eval A) -> A (since A is not defined, it evaluates to itself)
        List<String> resultsSymbol = interpretToStr(space, "(eval A)");
        assertEquals(List.of("A"), resultsSymbol);

        // (eval (foo bar)) -> (foo bar)
        List<String> resultsExpr = interpretToStr(space, "(eval (foo bar))");
        assertEquals(List.of("(foo bar)"), resultsExpr);
        
        // (eval $x) -> $x
        List<String> resultsVar = interpretToStr(space, "(eval $x)");
        assertEquals(List.of("$x"), resultsVar);

        // (eval {42}) -> {42} (GroundedAtom that's not Executable)
        // Note: {42} is not standard SExprParser syntax. We create GroundedAtoms directly or via specific symbols.
        // For testing, we can add a grounded atom to the space and try to eval a symbol that maps to it.
        // Or, if we had a syntax like `#grounded {42}` that parser understood.
        // For now, let's assume a symbol 'gnd-val' is defined via (= gnd-val <GroundedAtom42>)
        // or that the interpreter can somehow be fed a GroundedAtom directly in the (eval ...)
        // The current SExprParser creates GroundedAtoms for numbers like 42.
        List<String> resultsGnd = interpretToStr(space, "(eval 42)");
        assertEquals(List.of("42"), resultsGnd); // GroundedAtom(42).toString() is "42"
    }

    @Test
    void simpleEqualityRule() {
        GroundingSpace space = createSpace("(= A B)");
        List<String> results = interpretToStr(space, "(eval A)");
        assertEquals(List.of("B"), results);
    }

    @Test
    void equalityRuleWithVariable() {
        GroundingSpace space = createSpace("(= (foo $x) (bar $x))");
        List<String> results = interpretToStr(space, "(eval (foo baz))");
        assertEquals(List.of("(bar baz)"), results);

        List<String> resultsNoMatch = interpretToStr(space, "(eval (foo baz qux))");
         assertEquals(List.of("(foo baz qux)"), resultsNoMatch); // No rule match, evaluates to self
    }
    
    @Test
    void equalityRuleRecursive() {
        GroundingSpace space = createSpace("(= (fact $n) (* $n (fact (- $n 1)))) (= (fact 0) 1)");
        // This test would require arithmetic ops (*, -) to be GroundedAtoms with Executable logic.
        // The interpreter logic for Executable GroundedAtoms as operators is basic.
        // For now, let's test a non-recursive part or a very simple one if arithmetic isn't there.
        // Test: (eval (fact 0)) -> 1
        List<String> resultsFact0 = interpretToStr(space, "(eval (fact 0))");
        // This will now work if arithmetic ops are parsed correctly
        // assertEquals(List.of("1"), resultsFact0); // Original assertion before arithmetic

        // With arithmetic, (fact 0) should work.
        // Let's test this properly in arithmeticOperationsTest and keep this as is for now,
        // or update it if we confirm arithmetic is available at this stage of test execution.
        // For now, the original test logic for (fact 0) without full arithmetic support:
        // if arithmetic is not yet fully working in this old test, it might yield "1" if (= (fact 0) 1) is directly matched.
        // if SExprParser changes how `*` and `-` are parsed globally, this test might change behavior.
        // Assuming this test was written when `*` and `-` were just symbols.
        // The new arithmetic tests will cover the (fact $n) with actual computation.
        // Let's update this test to reflect that (fact 0) should simply return 1 due to the direct rule.
        assertEquals(List.of("1"), resultsFact0);


        GroundingSpace space2 = createSpace("(= (next A) B) (= (next B) C)");
        List<String> resultsNextA = interpretToStr(space2, "(eval (next A))");
        assertEquals(List.of("B"), resultsNextA);
        
        List<String> resultsNextNextA = interpretToStr(space2, "(eval (next (next A)))");
         assertEquals(List.of("C"), resultsNextNextA);
    }


    @Test
    void chainOperation() {
        GroundingSpace space = createSpace("(= (get-val) VAL)");
        List<String> results = interpretToStr(space, "(chain (eval (get-val)) $x (result $x))");
        assertEquals(List.of("(result VAL)"), results);

        GroundingSpace emptySpace = createSpace("");
        List<String> resultsNoVal = interpretToStr(emptySpace, "(chain (eval (get-val-missing)) $x (result $x))");
        assertEquals(List.of("(result (get-val-missing))"), resultsNoVal);
    }
    
    @Test
    void chainWithFurtherEval() {
        GroundingSpace space = createSpace("(= (get-val) (computation-needed)) (= (computation-needed) RESULT)");
        List<String> results = interpretToStr(space, "(chain (eval (get-val)) $x (eval $x))");
        assertEquals(List.of("RESULT"), results);
    }


    @Test
    void unifyOperation() {
        GroundingSpace space = createSpace("");
        
        List<String> resultsMatch = interpretToStr(space, "(unify (A $x) (A B) (then $x) (else))");
        assertEquals(List.of("(then B)"), resultsMatch);

        List<String> resultsNoMatch = interpretToStr(space, "(unify (A C) (A B) (then $x) (else))");
        assertEquals(List.of("(else)"), resultsNoMatch);
        
        List<String> resultsSimpleThen = interpretToStr(space, "(unify (A $x) (A B) (success) (failure))");
        assertEquals(List.of("(success)"), resultsSimpleThen);
    }
    
    @Test
    void deconsAndConsOperations() {
        GroundingSpace space = createSpace("");

        List<String> deconsResults = interpretToStr(space, "(eval (decons (a b c) $x $y $z))");
        assertEquals(List.of("(a b c)"), deconsResults);

        List<String> deconsFail = interpretToStr(space, "(eval (decons (a b) $x $y $z))");
        assertTrue(deconsFail.get(0).startsWith("(Error DeconsArityMismatch") || deconsFail.get(0).startsWith("(Error IncorrectArguments (decons (a b) $x $y $z))"));

        List<String> consResults = interpretToStr(space, "(eval (cons a b c))");
        assertEquals(List.of("(a b c)"), consResults);

        List<String> consWithVars = interpretToStr(space, "(eval (cons $x $y))");
        assertEquals(List.of("($x $y)"), consWithVars);
    }
    
    @Test
    void errorHandlingNotEnoughArgsForUnify() {
        GroundingSpace space = createSpace("");
        List<String> results = interpretToStr(space, "(eval (unify (A B)))");
        assertEquals(1, results.size());
        assertTrue(results.get(0).startsWith("(Error IncorrectArguments"));
    }

    private final String turingMachineBaseRules =
        "(= (tm $F $S ($L $C $R)) (chain (eval ($F $S $C)) $res (eval (apply-tm-result $res ($L $C $R) $F))))" +
        "(= (apply-tm-result ($NSym $NState $Dir) ($L $CSym $R) $F) (chain (eval (move $Dir ($L ($NSym) $R))) $newTape (tm $F $NState $newTape)))" +
        "(= (apply-tm-result ($NSym halt $Dir) ($L $CSym $R) $F) (chain (eval (move $Dir ($L ($NSym) $R))) $finalTape $finalTape))" +
        "(= (move L (($LH $LT) $C $R)) ($LT ($LH) ($C $R)))" +
        "(= (move L (() $C $R)) (() (blank) ($C $R)))" +
        "(= (move R ($L $C ($RH $RT))) (($C $L) ($RH) $RT))" +
        "(= (move R ($L $C ())) (($C $L) (blank) ()))";

    private final String busyBeaverRules =
        "(= (busy-beaver A blank) (1 B R))" +
        "(= (busy-beaver A 0)     (1 B R))" +
        "(= (busy-beaver A 1)     (1 C L))" +
        "(= (busy-beaver B blank) (1 A L))" +
        "(= (busy-beaver B 0)     (1 A L))" +
        "(= (busy-beaver B 1)     (1 B R))" +
        "(= (busy-beaver C blank) (1 B L))" +
        "(= (busy-beaver C 0)     (1 B L))" +
        "(= (busy-beaver C 1)     (halt halt R))";

    @Test
    void turingMachineSimplifiedStep() {
        GroundingSpace space = createSpace(turingMachineBaseRules + busyBeaverRules + "(= blank blank)");
        
        List<String> results = interpretToStr(space, "(eval (tm busy-beaver A (() blank ())))");
        
        assertFalse(results.isEmpty(), "Turing machine interpretation should produce a result.");
        Atom resultAtom = atom(results.get(0));
        assertTrue(resultAtom instanceof ExpressionAtom, "Final result of TM should be a tape (ExpressionAtom)");
        System.out.println("TM Result: " + results.get(0));
        assertFalse(results.get(0).startsWith("(Error"), "Turing machine execution should not result in an error");
    }

    @Test
    void arithmeticOperationsTest() {
        GroundingSpace space = createSpace(""); // Empty space for most direct eval tests

        // Basic Integer Operations
        assertEquals(List.of("5"), interpretToStr(space, "(eval (+ 2 3))"));
        assertEquals(List.of("3"), interpretToStr(space, "(eval (- 5 2))"));
        assertEquals(List.of("12"), interpretToStr(space, "(eval (* 3 4))"));

        // Basic Floating Point Operations
        assertEquals(List.of("6.0"), interpretToStr(space, "(eval (+ 2.5 3.5))"));
        assertEquals(List.of("2.5"), interpretToStr(space, "(eval (- 5.0 2.5))"));
        assertEquals(List.of("3.0"), interpretToStr(space, "(eval (* 1.5 2.0))"));
        assertEquals(List.of("3.5"), interpretToStr(space, "(eval (+ 1 2.5))")); // Mixed types
        assertEquals(List.of("-1.0"), interpretToStr(space, "(eval (- 1.0 2))")); // Mixed types subtraction

        // Nested Operations
        assertEquals(List.of("7"), interpretToStr(space, "(eval (+ 1 (* 2 3)))"));
        assertEquals(List.of("12"), interpretToStr(space, "(eval (* (+ 1 2) (- 5 1)))"));
        assertEquals(List.of("7.0"), interpretToStr(space, "(eval (+ 1.0 (* 2 3.0)))"));

        // Operations with Variables (using rules)
        GroundingSpace spaceArithVars = createSpace("(= (add-twice $x) (+ $x $x))");
        assertEquals(List.of("6"), interpretToStr(spaceArithVars, "(eval (add-twice 3))"));
        assertEquals(List.of("5.0"), interpretToStr(spaceArithVars, "(eval (add-twice 2.5))"));

        // Factorial Example (Recursive)
        GroundingSpace spaceFact = createSpace("(= (fact 0) 1) (= (fact $n) (* $n (fact (- $n 1))))");
        assertEquals(List.of("1"), interpretToStr(spaceFact, "(eval (fact 0))"));
        assertEquals(List.of("1"), interpretToStr(spaceFact, "(eval (fact 1))"));
        assertEquals(List.of("2"), interpretToStr(spaceFact, "(eval (fact 2))"));
        assertEquals(List.of("6"), interpretToStr(spaceFact, "(eval (fact 3))"));
        assertEquals(List.of("120"), interpretToStr(spaceFact, "(eval (fact 5))"));

        // Error Handling
        List<String> errorResult;

        // Incorrect number of arguments
        errorResult = interpretToStr(space, "(eval (+ 1))");
        assertEquals(1, errorResult.size(), "Expected one result for (+ 1)");
        assertTrue(errorResult.get(0).startsWith("(Error IncorrectArguments +)"), "Error msg mismatch for (+ 1). Was: " + errorResult.get(0));

        errorResult = interpretToStr(space, "(eval (- 1 2 3))");
        assertEquals(1, errorResult.size(), "Expected one result for (- 1 2 3)");
        assertTrue(errorResult.get(0).startsWith("(Error IncorrectArguments -)"), "Error msg mismatch for (- 1 2 3). Was: " + errorResult.get(0));

        // Non-numeric arguments
        errorResult = interpretToStr(space, "(eval (+ 1 A))");
        assertEquals(1, errorResult.size(), "Expected one result for (+ 1 A)");
        assertTrue(errorResult.get(0).startsWith("(Error NonNumericArguments + 1 A)"), "Error msg mismatch for (+ 1 A). Was: " + errorResult.get(0));

        errorResult = interpretToStr(space, "(eval (* B C))");
        assertEquals(1, errorResult.size(), "Expected one result for (* B C)");
        assertTrue(errorResult.get(0).startsWith("(Error NonNumericArguments * B C)"), "Error msg mismatch for (* B C). Was: " + errorResult.get(0));

        // Edge Cases
        assertEquals(List.of("-1"), interpretToStr(space, "(eval (- 2 3))"));
        assertEquals(List.of("0"), interpretToStr(space, "(eval (* 10 0))"));
        assertEquals(List.of("0.0"), interpretToStr(space, "(eval (* 10 0.0))"));
        assertEquals(List.of("0"), interpretToStr(space, "(eval (* 0 10))"));

        // Test with larger numbers (Long)
        assertEquals(List.of("3000000000"), interpretToStr(space, "(eval (* 300000 10000))")); // 3 * 10^9
        assertEquals(List.of(Long.toString(Long.MAX_VALUE)), interpretToStr(space, "(eval (+ " + (Long.MAX_VALUE - 1) + " 1))" ));
        assertEquals(List.of(Long.toString(Long.MIN_VALUE)), interpretToStr(space, "(eval (+ " + Long.MAX_VALUE + " 1))" ));
    }
}
