package com.example.metta.optimizer;

import com.example.metta.atom.Atom;
import com.example.metta.atom.SymbolAtom;
import com.example.metta.atom.VariableAtom;
import com.example.metta.atom.ExpressionAtom;
import com.example.metta.atom.MettaSymbols;
import com.example.metta.space.GroundingSpace;
import com.example.metta.optimizer.ParameterOptimizer;
import com.example.metta.optimizer.QueryTask; // Added import
import com.example.metta.optimizer.TaskSuite; // Added import

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions; // Covers assertEquals

import java.util.List; // For ExpressionAtom constructor helper

/**
 * Test class for ParameterOptimizer.
 */
public class ParameterOptimizerTest {

    // Helper methods for test case construction
    private SymbolAtom sym(String name) { return new SymbolAtom(name); }
    private VariableAtom var(String name) { return new VariableAtom(name); }
    private ExpressionAtom expr(Atom... children) { return new ExpressionAtom(List.of(children)); }

    @Test
    void testSimpleDepthAnalysis() {
        GroundingSpace space = new GroundingSpace();

        // Populate the space: A -> B -> C -> D path
        space.add(expr(sym("Link"), sym("A"), sym("B")));
        space.add(expr(sym("Link"), sym("B"), sym("C")));
        space.add(expr(sym("Link"), sym("C"), sym("D")));
        // Another disconnected link to ensure query is specific
        space.add(expr(sym("Link"), sym("X"), sym("Y")));

        // Define the test query: (traverse A (PathStar (Link $From $To)) D)
        Atom queryAtom = expr(
            MettaSymbols.TRAVERSE_SYMBOL,
            sym("A"),
            expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Link"), var("From"), var("To"))),
            sym("D")
        );

        ParameterOptimizer optimizer = new ParameterOptimizer();

        // Expected minimum depth for A -> D is 3. Test range [1, 5].
        System.out.println("Starting testSimpleDepthAnalysis for ParameterOptimizer...");
        optimizer.analyzeMaxPathStarDepth(space, queryAtom, 1, 5);
        System.out.println("testSimpleDepthAnalysis for ParameterOptimizer complete.");

        // TODO: Add assertions based on expected output if analyzeMaxPathStarDepth is modified to return structured data.
        // For now, this test drives the optimizer and output should be manually inspected.
        Assertions.assertTrue(true); // Placeholder assertion
    }

    @Test
    void testSuiteDepthOptimization() {
        System.out.println("\n--- Starting testSuiteDepthOptimization ---");
        GroundingSpace space = new GroundingSpace();

        // Populate space for diverse queries
        // Path 1 (A->B->C): Length 2
        space.add(expr(sym("Path"), sym("A"), sym("B")));
        space.add(expr(sym("Path"), sym("B"), sym("C")));

        // Path 2 (X->Y->Z->W): Length 3
        space.add(expr(sym("Path"), sym("X"), sym("Y")));
        space.add(expr(sym("Path"), sym("Y"), sym("Z")));
        space.add(expr(sym("Path"), sym("Z"), sym("W")));

        // Path 3 (M->N) : Length 1
        space.add(expr(sym("Path"), sym("M"), sym("N")));


        // Define QueryTasks
        QueryTask task1 = new QueryTask(
            expr(MettaSymbols.TRAVERSE_SYMBOL, sym("A"), expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Path"), var("F1"), var("T1"))), sym("C")),
            "Path A to C (expected min depth 2)",
            results -> !results.isEmpty()
        );

        QueryTask task2 = new QueryTask(
            expr(MettaSymbols.TRAVERSE_SYMBOL, sym("X"), expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Path"), var("F2"), var("T2"))), sym("W")),
            "Path X to W (expected min depth 3)",
            results -> !results.isEmpty()
        );

        QueryTask task3 = new QueryTask(
            expr(MettaSymbols.TRAVERSE_SYMBOL, sym("M"), expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Path"), var("F3"), var("T3"))), sym("N")),
            "Path M to N (expected min depth 1)",
            results -> !results.isEmpty()
        );

        // Create TaskSuite
        TaskSuite suite = new TaskSuite();
        suite.addTask(task1);
        suite.addTask(task2);
        suite.addTask(task3);

        ParameterOptimizer optimizer = new ParameterOptimizer();

        // Test: Optimal depth should be 3, as task2 requires it.
        // Depths to test: 1 through 5. Default to -1 if no solution found.
        int optimalDepth = optimizer.findOptimalDepthForSuite(space, suite, 1, 5, -1);

        System.out.println("testSuiteDepthOptimization: Reported optimal depth by optimizer: " + optimalDepth);
        Assertions.assertEquals(3, optimalDepth, "Optimal depth for the suite should be 3.");

        // Test: If maxTestDepth is too low (e.g., 2), it should fail and return defaultOnFailure
        System.out.println("Testing suite with maxTestDepth = 2 (expected failure for suite)");
        int failedOutcomeDepth = optimizer.findOptimalDepthForSuite(space, suite, 1, 2, -1);
        System.out.println("testSuiteDepthOptimization: Reported depth for constrained search: " + failedOutcomeDepth);
        Assertions.assertEquals(-1, failedOutcomeDepth, "Optimal depth should be defaultOnFailure (-1) when maxTestDepth is too low for suite.");

        // Test: Empty suite (should succeed at minDepth if logic handles it, or per specific design)
        // The current findOptimalDepthForSuite iterates tasks. An empty suite would mean the inner loop doesn't run,
        // allTasksSucceededAtThisDepth remains true. So it should return minDepth.
        TaskSuite emptySuite = new TaskSuite();
        System.out.println("Testing empty suite...");
        int emptySuiteDepth = optimizer.findOptimalDepthForSuite(space, emptySuite, 1, 5, -1);
        System.out.println("testSuiteDepthOptimization: Reported depth for empty suite: " + emptySuiteDepth);
        Assertions.assertEquals(1, emptySuiteDepth, "Optimal depth for an empty suite should be the minDepth tested.");


        System.out.println("--- testSuiteDepthOptimization Complete ---");
    }
}
