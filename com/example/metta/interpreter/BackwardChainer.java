package com.example.metta.interpreter;

import com.example.metta.atom.*;
import com.example.metta.matcher.Matcher;
import com.example.metta.space.SpaceReader;
import com.example.metta.types.Bindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BackwardChainer {

    private final SpaceReader space;
    private final int maxDepth;

    public BackwardChainer(SpaceReader space, int maxDepth) {
        this.space = space;
        this.maxDepth = maxDepth;
    }

    public List<Bindings> prove(Atom query) {
        return proveRecursive(query, new Bindings(), 0);
    }

    // Overloaded method to accept initial bindings if needed later by other processes
    public List<Bindings> prove(Atom query, Bindings initialBindings) {
        return proveRecursive(query, initialBindings, 0);
    }

    private List<Atom> getRulesFromSpace() {
        List<Atom> rules = new ArrayList<>();
        // Assuming space.getAtoms() returns all atoms. This might be inefficient for large spaces.
        // In a real scenario, rules might be indexed or stored separately.
        for (Atom atom : space.getAtoms()) {
            if (atom instanceof ExpressionAtom) {
                ExpressionAtom expr = (ExpressionAtom) atom;
                if (expr.getChildren().size() == 3 && expr.getChildren().get(0).equals(MettaSymbols.IMPLIES_SYMBOL)) {
                    rules.add(expr);
                }
            }
        }
        return rules;
    }

    private List<Bindings> proveRecursive(Atom query, Bindings currentBindings, int depth) {
        if (depth > maxDepth) {
            return Collections.emptyList();
        }

        Atom concreteQuery = Matcher.applyBindings(query, currentBindings);

        // Handle logical connectives
        if (concreteQuery instanceof ExpressionAtom) {
            ExpressionAtom exprQuery = (ExpressionAtom) concreteQuery;
            if (!exprQuery.getChildren().isEmpty()) {
                Atom operator = exprQuery.getChildren().get(0);

                if (operator.equals(MettaSymbols.AND_SYMBOL)) {
                    // Children of ExpressionAtom include the operator itself.
                    // So, subList(1, ...) gives the actual arguments.
                    return proveAnd(exprQuery.getChildren().subList(1, exprQuery.getChildren().size()), currentBindings, depth);
                } else if (operator.equals(MettaSymbols.OR_SYMBOL)) {
                    return proveOr(exprQuery.getChildren().subList(1, exprQuery.getChildren().size()), currentBindings, depth);
                } else if (operator.equals(MettaSymbols.NOT_SYMBOL)) {
                    if (exprQuery.getChildren().size() == 2) { // Expecting (not <atom>)
                        return proveNot(exprQuery.getChildren().get(1), currentBindings, depth);
                    } else {
                        // Ill-formed (not) or (not A B ...), treat as unprovable.
                        return Collections.emptyList();
                    }
                }
            }
        }
        // If not a recognized logical connective, proceed to existing fact/rule matching logic.

        List<Bindings> allResults = new ArrayList<>();

        // Fact Matching (Base Case)
        // space.query is expected to return bindings that make the concreteQuery match a fact.
        // If concreteQuery is ground, it returns an empty binding if matched, or empty list.
        // If concreteQuery has variables, it binds them.
        List<Bindings> spaceQueryResults = space.query(concreteQuery);
        for (Bindings bindingFromSpace : spaceQueryResults) {
            Bindings newPotentialBindings = currentBindings.copy();
            List<Bindings> mergedFactBindings = newPotentialBindings.merge(bindingFromSpace);
            // merge returns a list; if empty, merge failed. If successful, it contains the merged binding.
            allResults.addAll(mergedFactBindings);
        }

        // Rule Matching (Recursive Step)
        List<Atom> rules = getRulesFromSpace();
        for (Atom rule : rules) {
            // Ensure it's a valid implication: (=> <condition> <conclusion>)
            // Type cast safety should be ensured by getRulesFromSpace, but a check wouldn't hurt.
            ExpressionAtom ruleExpr = (ExpressionAtom) rule;
            Atom condition = ruleExpr.getChildren().get(1);
            Atom conclusionTemplate = ruleExpr.getChildren().get(2); // Rule's conclusion pattern

            // Try to match the rule's conclusion with the current concreteQuery
            // This finds what the rule's conclusion would need to bind to match our query.
            List<Bindings> unificationResults = Matcher.matchAtoms(conclusionTemplate, concreteQuery);

            for (Bindings unificationBinding : unificationResults) {
                // unificationBinding contains bindings for variables in conclusionTemplate
                // relative to concreteQuery.
                // We need to merge these with currentBindings to get bindings for the rule's condition.
                Bindings bindingsForSubgoal = currentBindings.copy();
                List<Bindings> mergedSubgoalBindingsList = bindingsForSubgoal.merge(unificationBinding);

                for (Bindings mergedBindingForSubgoal : mergedSubgoalBindingsList) {
                    // Now prove the rule's condition (newSubGoal) using these merged bindings.
                    // The condition itself is the pattern for the subgoal.
                    List<Bindings> subProofResults = proveRecursive(condition, mergedBindingForSubgoal, depth + 1);

                    // subProofResults are bindings that satisfy the condition.
                    // These bindings are extensions of mergedBindingForSubgoal.
                    // Since mergedBindingForSubgoal already includes currentBindings and unificationBinding,
                    // subProofResults are directly the final results for this path.
                    allResults.addAll(subProofResults);
                }
            }
        }

        // Consider removing duplicate Bindings objects if necessary, for now, return all.
        // A HashSet could be used if Bindings implements hashCode and equals properly.
        // For example: return new ArrayList<>(new HashSet<>(allResults));
        return allResults;
    }

    private List<Bindings> proveAnd(List<Atom> conjuncts, Bindings currentBindings, int depth) {
        // Note: depth here is the depth of the (and ...) expression itself.
        // Calls to proveRecursive for conjuncts will handle their own depth increments if they recurse further.
        if (conjuncts.isEmpty()) {
            // An empty list of conjuncts is typically true.
            return Collections.singletonList(currentBindings);
        }

        List<Bindings> finalResults = new ArrayList<>();
        Atom firstConjunct = conjuncts.get(0);
        List<Atom> remainingConjuncts = conjuncts.subList(1, conjuncts.size());

        // Prove the first conjunct using the current depth.
        // proveRecursive itself will use 'depth + 1' if it makes further recursive calls (e.g. for rules).
        List<Bindings> firstConjunctResults = proveRecursive(firstConjunct, currentBindings, depth);

        for (Bindings b : firstConjunctResults) {
            // If the first conjunct was proven, try to prove the rest with the resulting bindings.
            // Pass the same 'depth' as we are still resolving parts of the same AND expression.
            // The recursive call to proveAnd will, in turn, call proveRecursive with this same 'depth'.
            if (remainingConjuncts.isEmpty()) { // if it was the last conjunct
                finalResults.add(b);
            } else {
                finalResults.addAll(proveAnd(remainingConjuncts, b, depth));
            }
        }
        return finalResults;
    }

    private List<Bindings> proveOr(List<Atom> disjuncts, Bindings currentBindings, int depth) {
        // Note: depth here is the depth of the (or ...) expression itself.
        List<Bindings> allOrResults = new ArrayList<>();
        if (disjuncts.isEmpty()) {
            // An empty list of disjuncts is typically false.
            return Collections.emptyList();
        }

        for (Atom disjunct : disjuncts) {
            // Each disjunct is a new attempt.
            // proveRecursive itself will use 'depth + 1' if it makes further recursive calls.
            allOrResults.addAll(proveRecursive(disjunct, currentBindings.copy(), depth)); // Use copy of currentBindings
        }
        // Consider removing duplicates if different proof paths for disjuncts yield identical bindings.
        // Example: return new ArrayList<>(new HashSet<>(allOrResults));
        return allOrResults;
    }

    private List<Bindings> proveNot(Atom negatedQuery, Bindings currentBindings, int depth) {
        // Note: depth here is the depth of the (not ...) expression itself.
        // Call proveRecursive for the negated query.
        // proveRecursive itself will use 'depth + 1' if it makes further recursive calls.
        List<Bindings> results = proveRecursive(negatedQuery, currentBindings, depth);

        if (results.isEmpty()) { // If negatedQuery FAILED to prove (is false)
            return Collections.singletonList(currentBindings); // Then (not negatedQuery) is true
        } else {
            return Collections.emptyList(); // If negatedQuery SUCCEEDED (is true), then (not negatedQuery) is false
        }
    }
}
