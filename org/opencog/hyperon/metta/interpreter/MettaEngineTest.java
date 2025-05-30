package org.opencog.hyperon.metta.interpreter;

import org.opencog.hyperon.atoms.*;
import org.opencog.hyperon.metta.parser.MettaParser;
import org.opencog.hyperon.space.AtomSpace;
import org.opencog.hyperon.space.Bindings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MettaEngineTest {

    private AtomSpace space;
    private MettaEngine engine;
    private MettaParser parser;

    // Helper methods for creating atoms in tests
    private SymbolAtom S(String name) {
        return new SymbolAtom(name);
    }

    private VariableAtom V(String name) {
        return new VariableAtom(name);
    }

    private ExpressionAtom E(Atom... atoms) {
        return new ExpressionAtom(Arrays.asList(atoms));
    }

    private AtomSpace space;
    private MettaEngine engine;
    private MettaParser parser;
    private GroundedAtomRegistry registry;

    @BeforeEach
    void setUp() {
        space = new AtomSpace();
        registry = new GroundedAtomRegistry();
        engine = new MettaEngine(space, registry);
        parser = new MettaParser();

        // Register common ops
        registry.registerOperation("+", new org.opencog.hyperon.metta.grounded_ops.arithmetic.AddOp());
        registry.registerOperation("-", new org.opencog.hyperon.metta.grounded_ops.arithmetic.SubtractOp());
        registry.registerOperation("*", new org.opencog.hyperon.metta.grounded_ops.arithmetic.MultiplyOp());
        registry.registerOperation("%", new org.opencog.hyperon.metta.grounded_ops.arithmetic.ModuloOp());
        registry.registerOperation("<", new org.opencog.hyperon.metta.grounded_ops.arithmetic.LessThanOp());
        registry.registerOperation(">", new org.opencog.hyperon.metta.grounded_ops.arithmetic.GreaterThanOp());
        registry.registerOperation("==", new org.opencog.hyperon.metta.grounded_ops.arithmetic.EqualsOp());
        registry.registerOperation("and", new org.opencog.hyperon.metta.grounded_ops.logic.AndOp());

        // Register 'if' as rules, as it's a core logical construct often defined in MeTTa itself
        loadScript("(= (if True $then $_else) $then)");
        loadScript("(= (if False $_then $else) $else)");
    }

    private void loadScript(String scriptContent) {
        List<Atom> atoms = parser.parseScript(scriptContent);
        atoms.forEach(space::addAtom);
    }

    @Test
    void testEvaluateSymbol() {
        List<Atom> result = engine.evaluate(S("A"), new Bindings());
        assertEquals(Collections.singletonList(S("A")), result);
    }

    @Test
    void testEvaluateVariableBound() {
        Bindings b = new Bindings().addBinding(V("$X"), S("A"));
        List<Atom> result = engine.evaluate(V("$X"), b);
        assertEquals(Collections.singletonList(S("A")), result);
    }

    @Test
    void testEvaluateVariableBoundToExpression() {
        ExpressionAtom exprBC = E(S("B"), S("C"));
        Bindings b = new Bindings().addBinding(V("$X"), exprBC);
        // engine.evaluate should further evaluate the bound value.
        // Since (B C) by itself evaluates to (B C) when no rules/ops,
        // the final result is (B C).
        List<Atom> result = engine.evaluate(V("$X"), b);
        assertEquals(Collections.singletonList(exprBC), result);
    }


    @Test
    void testEvaluateVariableUnbound() {
        List<Atom> result = engine.evaluate(V("$X"), new Bindings());
        assertEquals(Collections.singletonList(V("$X")), result);
    }

    @Test
    void testEvaluateExpressionAsData() {
        ExpressionAtom expr = E(S("A"), S("B"));
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(expr), result);
    }

    @Test
    void testEvaluateEmptyExpression() {
        ExpressionAtom emptyExpr = E();
        List<Atom> result = engine.evaluate(emptyExpr, new Bindings());
        assertEquals(Collections.singletonList(emptyExpr), result);
    }


    @Test
    void testEvaluateGroundedAtomDirectly() {
        Groundable op = args -> S("Result"); // Simple operation
        GroundedAtom gAtom = new GroundedAtom("TestOp", op);
        List<Atom> result = engine.evaluate(gAtom, new Bindings());
        // GroundedAtoms evaluated directly (not as head of expr) should return themselves
        assertEquals(Collections.singletonList(gAtom), result);
    }

    @Test
    void testEvaluateGroundedExpression() {
        Groundable addOp = args -> {
            int sum = 0;
            for (Atom arg : args) {
                if (arg instanceof SymbolAtom) { // Assuming numbers are represented as symbols for this test
                    sum += Integer.parseInt(arg.getName());
                } else {
                    throw new UnsupportedOperationException("Invalid arg type for test addOp");
                }
            }
            return S(String.valueOf(sum));
        };
        GroundedAtom plusAtom = new GroundedAtom("plus", addOp);
        ExpressionAtom expr = E(plusAtom, S("1"), S("2"));

        List<Atom> result = engine.evaluate(expr, new Bindings());
        // Result of plusAtom.executeOperation is S("3").
        // engine.evaluate(S("3"), bindings) returns [S("3")]
        assertEquals(Collections.singletonList(S("3")), result);
    }

    @Test
    void testEvaluateGroundedExpressionWithVariableArgument() {
        Groundable stringConcatOp = args -> {
            StringBuilder sb = new StringBuilder();
            for(Atom arg : args) {
                // Assumes args are resolved to Symbols or simple types convertible to string
                if (arg instanceof SymbolAtom) sb.append(arg.getName());
                else if (arg instanceof VariableAtom) sb.append(arg.getName()); // Should be resolved ideally
                else throw new RuntimeException("Unsupported arg type for concatOp: " + arg.getType());
            }
            return S(sb.toString());
        };
        GroundedAtom concatAtom = new GroundedAtom("concat", stringConcatOp);

        Bindings bindings = new Bindings().addBinding(V("$X"), S("Hello"));
        ExpressionAtom expr = E(concatAtom, V("$X"), S("World")); // (concat $X World)

        List<Atom> result = engine.evaluate(expr, bindings);
        // $X becomes "Hello". Args to concatOp: [S("Hello"), S("World")]
        // concatOp returns S("HelloWorld"). evaluate(S("HelloWorld"), ...) -> [S("HelloWorld")]
        assertEquals(Collections.singletonList(S("HelloWorld")), result);
    }


    @Test
    void testSimpleRuleApplication() {
        // Rule: (= (foo $X) (bar $X))
        // Query: (foo A)
        // Expected result: (bar A)
        space.addAtom(parser.parseAtom("(= (foo $X) (bar $X))"));
        Atom input = parser.parseAtom("(foo A)");
        List<Atom> result = engine.evaluate(input, new Bindings());

        assertEquals(1, result.size());
        assertEquals(parser.parseAtom("(bar A)"), result.get(0));
    }

    @Test
    void testRuleApplicationLeadsToSymbol() {
        // Rule: (= (get-name) Name)
        // Query: (get-name)
        // Expected result: Name
        space.addAtom(parser.parseAtom("(= (get-name) Name)"));
        Atom input = parser.parseAtom("(get-name)");
        List<Atom> result = engine.evaluate(input, new Bindings());

        assertEquals(1, result.size());
        assertEquals(S("Name"), result.get(0));
    }


    @Test
    void testChainedRuleApplication() {
        // Rules:
        // (= (foo $X) (bar $X))
        // (= (bar $Y) (baz $Y))
        // Query: (foo A)
        // Expected: (baz A)
        // foo A -> bar A (by rule 1)
        // bar A -> baz A (by rule 2)
        space.addAtom(parser.parseAtom("(= (foo $X) (bar $X))"));
        space.addAtom(parser.parseAtom("(= (bar $Y) (baz $Y))"));
        Atom input = parser.parseAtom("(foo A)");

        List<Atom> result = engine.evaluate(input, new Bindings());
        assertEquals(1, result.size());
        assertEquals(parser.parseAtom("(baz A)"), result.get(0));
    }

    @Test
    void testRuleWithGroundedAtomInRHS() {
        // Rule: (= (calc-sum $A $B) (do-plus $A $B))
        // where (do-plus $A $B) will be an expression with a GroundedAtom head
        Groundable addOp = args -> S(String.valueOf(
                Integer.parseInt(((SymbolAtom)args.get(0)).getName()) +
                Integer.parseInt(((SymbolAtom)args.get(1)).getName())));
        GroundedAtom plusAtom = new GroundedAtom("internalPlus", addOp); // Name here is just for debugging

        // We need to evaluate (internalPlus $A $B) where internalPlus is plusAtom.
        // The rule RHS will be (internalPlus $A $B).
        // The engine currently doesn't resolve SymbolAtom "internalPlus" to plusAtom.
        // So, for this test, we'll make the rule produce an expression that, when evaluated,
        // can be directly executed if we put the actual GroundedAtom in the rule's RHS.
        // This is a bit of a hack for the current engine limitations.
        // A better way would be registering "internalPlus" symbol to resolve to plusAtom.

        // Let's define a rule that produces an expression known to the test,
        // and then evaluate that expression if it contains the GroundedAtom directly.
        // Rule: (= (request-sum $A $B) (prepare-sum $A $B))
        // Then we evaluate (prepare-sum 1 2) which should resolve to (plusAtom 1 2) -> 3 if plusAtom is in scope.
        // This is still tricky.

        // Simpler: Rule RHS *is* the executable grounded expression.
        // (= (call-plus $A $B) ( (plus) $A $B ) )  <- This means (plus) should be the GroundedAtom itself.
        // This is not directly parsable.

        // Let's make a rule whose RHS is a simple symbol that we then evaluate,
        // and that simple symbol is the head of a grounded expression.
        // Rule: (= (ask-for-op) actual-op)
        // Then evaluate (actual-op 5 7)
        space.addAtom(E(S("="), E(S("ask-for-op")), S("actual-op")));

        // Now, if we evaluate (ask-for-op), we get [S("actual-op")]
        // If we then evaluate (actual-op 5 7) with "actual-op" being resolvable to plusAtom, it'd work.
        // The current engine doesn't do symbol-to-groundedAtom resolution in the head.
        // The GroundedAtom must be literally the head of the expression.

        // Test case: Rule's RHS is an expression that *is* a grounded call.
        // To do this, we need to insert the GroundedAtom instance into the rule's RHS.
        // This cannot be done with parser.parseAtom for the whole rule if GroundedAtom is not parsable.
        // Let `plusWrapper` be a symbol. Rule `(= (call-plus $A $B) (plusWrapper $A $B))`.
        // Then, if `(plusWrapper 1 2)` is evaluated, and `plusWrapper` is the GroundedAtom `plusAtom`.

        // Let's assume the GroundedAtom *is* the head symbol in the rule's RHS.
        // This implies that the parser would need to be able to produce GroundedAtoms
        // or they are added programmatically.
        // For the test, let's add the rule with the GroundedAtom directly in the RHS.
        VariableAtom varA = V("$A");
        VariableAtom varB = V("$B");
        space.addAtom(E(S("="), E(S("call-plus"), varA, varB), E(plusAtom, varA, varB)));

        Atom input = E(S("call-plus"), S("10"), S("20"));
        List<Atom> result = engine.evaluate(input, new Bindings());
        // 1. (call-plus 10 20) matches rule. $A=10, $B=20. $Res = (plusAtom 10 20)
        // 2. evaluate((plusAtom 10 20), initial_bindings)
        //    head is plusAtom. args are 10, 20.
        //    evaluate(10,{}) -> [10]. evaluate(20,{}) -> [20]
        //    plusAtom.execute([10, 20]) -> S("30")
        //    evaluate(S("30"), {}) -> [S("30")]
        assertEquals(Collections.singletonList(S("30")), result);
    }

    @Test
    void testRuleApplicationWithBindings() {
        // Rule: (= (foo $X) (bar $X))
        // Query: (foo $Y) with $Y bound to A
        // Expected: (bar A)
        space.addAtom(parser.parseAtom("(= (foo $X) (bar $X))"));

        Bindings initialBindings = new Bindings().addBinding(V("$Y"), S("A"));
        Atom input = E(S("foo"), V("$Y")); // (foo $Y)

        List<Atom> result = engine.evaluate(input, initialBindings);
        // 1. Evaluate (foo $Y) with $Y=A. Becomes (foo A).
        // 2. (foo A) matches (= (foo $X) (bar $X)). matchBinding: $X=A, $Res=(bar $X)
        // 3. ruleBody = (bar $X)
        // 4. partiallySubstitutedBody = matchBinding.substitute((bar $X)) = (bar A)
        // 5. evaluate((bar A), initialBindings {$Y=A}) -> results in (bar A)
        assertEquals(Collections.singletonList(parser.parseAtom("(bar A)")), result);
    }

    // New Tests for Registered Grounded Operations
    @Test
    void testRegisteredAddition() {
        // registry is already set up with "+" in setUp()
        Atom expr = parser.parseAtom("(+ 10 5)");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("15")), result);
    }

    @Test
    void testRegisteredSubtraction() {
        // registry is already set up with "-" in setUp()
        Atom expr = parser.parseAtom("(- 10 5)");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("5")), result);
    }

    @Test
    void testRegisteredAdditionWithNegativeResult() {
        Atom expr = parser.parseAtom("(+ 5 -10)");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("-5")), result);
    }

    @Test
    void testRegisteredEqualsTrue() {
        // "==" is registered in setUp()
        Atom expr = parser.parseAtom("(== (A B) (A B))");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("True")), result);
    }

    @Test
    void testRegisteredEqualsFalse() {
        Atom expr = parser.parseAtom("(== (A B) (A C))");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("False")), result);
    }

    @Test
    void testRegisteredEqualsWithDifferentTypes() {
        Atom expr = parser.parseAtom("(== A (A))"); // Symbol vs Expression
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("False")), result);
    }

    @Test
    void testRuleApplicationWithRegisteredGroundedOpInRHS() {
        // Ops like "+" are registered in setUp()
        loadScript("(= (add-val $X $Y) (+ $X $Y))");
        Atom input = parser.parseAtom("(add-val 7 8)");
        List<Atom> result = engine.evaluate(input, new Bindings());
        assertEquals(Collections.singletonList(S("15")), result);
    }

    @Test
    void testGroundedOpArgumentEvaluationViaRulesAndRegistry() {
        // Ops like "+" are registered in setUp()
        loadScript("(= (get-five) 5)");
        loadScript("(= (get-ten) 10)");
        Atom expr = parser.parseAtom("(+ (get-ten) (get-five))");
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertEquals(Collections.singletonList(S("15")), result);
    }

    @Test
    void testGroundedOpArgumentFailedEvaluation() {
        // Ops like "+" are registered in setUp()
        Atom expr = parser.parseAtom("(+ 5 (get-fail))"); // (get-fail) is undefined
        List<Atom> result = engine.evaluate(expr, new Bindings());
        assertTrue(result.isEmpty(), "Expected empty result due to error in grounded op execution from arg eval failure.");
    }

    // Factorial Test
    @Test
    void testFactorial() {
        // Ops *, ==, - are registered in setUp()
        // If rules are already loaded in setUp, ensure they don't clash or load them here.
        // (= (factorial $N) (if (== $N 0) 1 (* $N (factorial (- $N 1)))))
        loadScript("(= (factorial $N) (if (== $N 0) 1 (* $N (factorial (- $N 1)))))");

        Atom input0 = parser.parseAtom("(factorial 0)");
        List<Atom> result0 = engine.evaluate(input0, new Bindings());
        assertEquals(Collections.singletonList(S("1")), result0, "Factorial of 0 should be 1");

        Atom input1 = parser.parseAtom("(factorial 1)");
        List<Atom> result1 = engine.evaluate(input1, new Bindings());
        assertEquals(Collections.singletonList(S("1")), result1, "Factorial of 1 should be 1");

        Atom input3 = parser.parseAtom("(factorial 3)");
        List<Atom> result3 = engine.evaluate(input3, new Bindings());
        assertEquals(Collections.singletonList(S("6")), result3, "Factorial of 3 should be 6");

        Atom input5 = parser.parseAtom("(factorial 5)");
        List<Atom> result5 = engine.evaluate(input5, new Bindings());
        assertEquals(Collections.singletonList(S("120")), result5, "Factorial of 5 should be 120");
    }

    // FizzBuzz-For-N Test
    @Test
    void testFizzBuzzForN() {
        // Ops %, ==, and, <, > are registered in setUp. If rules are in setUp.
        // (= (divides $K $N) (== (% $N $K) 0))
        // (= (fizzbuzz-for $N)
        //    (if (and (divides 3 $N) (divides 5 $N)) (FizzBuzz $N)
        //    (if (divides 3 $N) (Fizz $N)
        //    (if (divides 5 $N) (Buzz $N)
        //        (Number $N))))) // Using (Number $N) to distinguish from plain $N

        loadScript("(= (divides $K $N) (== (% $N $K) 0))");
        loadScript("(= (fizzbuzz-for $N) (if (and (divides 3 $N) (divides 5 $N)) (FizzBuzz $N) (if (divides 3 $N) (Fizz $N) (if (divides 5 $N) (Buzz $N) (Number $N)))))");

        Atom input1 = parser.parseAtom("(fizzbuzz-for 1)");
        List<Atom> result1 = engine.evaluate(input1, new Bindings());
        assertEquals(Collections.singletonList(E(S("Number"), S("1"))), result1);

        Atom input2 = parser.parseAtom("(fizzbuzz-for 2)");
        List<Atom> result2 = engine.evaluate(input2, new Bindings());
        assertEquals(Collections.singletonList(E(S("Number"), S("2"))), result2);

        Atom input3 = parser.parseAtom("(fizzbuzz-for 3)");
        List<Atom> result3 = engine.evaluate(input3, new Bindings());
        assertEquals(Collections.singletonList(E(S("Fizz"), S("3"))), result3);

        Atom input5 = parser.parseAtom("(fizzbuzz-for 5)");
        List<Atom> result5 = engine.evaluate(input5, new Bindings());
        assertEquals(Collections.singletonList(E(S("Buzz"), S("5"))), result5);

        Atom input6 = parser.parseAtom("(fizzbuzz-for 6)");
        List<Atom> result6 = engine.evaluate(input6, new Bindings());
        assertEquals(Collections.singletonList(E(S("Fizz"), S("6"))), result6);

        Atom input10 = parser.parseAtom("(fizzbuzz-for 10)");
        List<Atom> result10 = engine.evaluate(input10, new Bindings());
        assertEquals(Collections.singletonList(E(S("Buzz"), S("10"))), result10);

        Atom input15 = parser.parseAtom("(fizzbuzz-for 15)");
        List<Atom> result15 = engine.evaluate(input15, new Bindings());
        assertEquals(Collections.singletonList(E(S("FizzBuzz"), S("15"))), result15);
    }
}
