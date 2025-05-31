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

public class ForwardChainer {

    public Set<Atom> trigger(SpaceReader space, Atom newFact, Set<Atom> existingFacts) {
        Set<Atom> newlyDerivedConclusions = new HashSet<>();
        List<Atom> rules = getRules(space); // Assumes getRules() is correct

        for (Atom rule : rules) {
            if (!(rule instanceof ExpressionAtom) || ((ExpressionAtom) rule).getChildren().size() != 3) {
                continue;
            }
            ExpressionAtom ruleExpr = (ExpressionAtom) rule;
            if (!ruleExpr.getChildren().get(0).equals(MettaSymbols.IMPLIES_SYMBOL)) {
                continue;
            }

            Atom conditionsAtom = ruleExpr.getChildren().get(1);
            Atom conclusionTemplate = ruleExpr.getChildren().get(2);

            // Pass existingFacts, which includes newFact.
            List<Bindings> possibleBindings = matchConditions(newFact, conditionsAtom, existingFacts);

            for (Bindings binding : possibleBindings) {
                Atom concreteConclusion = Matcher.applyBindings(conclusionTemplate, binding);
                // Check against existingFacts snapshot passed to trigger.
                // The GroundingSpace.add method will handle the ultimate check against its live atom set.
                if (!existingFacts.contains(concreteConclusion) && !newlyDerivedConclusions.contains(concreteConclusion)) {
                    newlyDerivedConclusions.add(concreteConclusion);
                }
            }
        }
        return newlyDerivedConclusions;
    }

    private List<Atom> getRules(SpaceReader space) {
        List<Atom> allAtoms = space.getAtoms();
        List<Atom> rules = new ArrayList<>();
        for (Atom atom : allAtoms) {
            if (atom instanceof ExpressionAtom) {
                ExpressionAtom expr = (ExpressionAtom) atom;
                if (!expr.getChildren().isEmpty() && expr.getChildren().get(0).equals(MettaSymbols.IMPLIES_SYMBOL)) {
                    if (expr.getChildren().size() == 3) { // Ensures basic (=> P Q) structure
                        rules.add(expr);
                    }
                }
            }
        }
        return rules;
    }

    private List<Bindings> matchConditions(Atom newFact, Atom conditionsAtom, Set<Atom> existingFacts) {
        if (conditionsAtom instanceof ExpressionAtom &&
            !((ExpressionAtom) conditionsAtom).getChildren().isEmpty() &&
            ((ExpressionAtom) conditionsAtom).getChildren().get(0).equals(MettaSymbols.AND_SYMBOL)) {

            List<Atom> conjuncts = ((ExpressionAtom) conditionsAtom).getChildren().subList(1, ((ExpressionAtom) conditionsAtom).getChildren().size());
            if (conjuncts.isEmpty()) { // An (And) with no conjuncts.
                return Collections.emptyList(); // Or perhaps a single empty binding if it means "true"? For now, treat as no trigger.
            }
            // For And conditions, newFact must be involved in satisfying one of the conjuncts,
            // and the rest must be satisfiable from existingFacts.
            return matchConjunctiveConditionsRecursive(newFact, conjuncts, existingFacts);
        } else {
            // Single condition: must be matched by newFact.
            List<Bindings> factMatchBindings = Matcher.matchAtoms(conditionsAtom, newFact);

            // Filter to ensure the binding results in an atom equal to newFact,
            // effectively confirming newFact itself satisfies the pattern.
            return factMatchBindings.stream()
                                    .filter(b -> {
                                        Atom boundPattern = Matcher.applyBindings(conditionsAtom, b);
                                        return newFact.equals(boundPattern);
                                        // We already know newFact is in existingFacts from the caller (GroundingSpace)
                                    })
                                    .collect(Collectors.toList());
        }
    }

    private List<Bindings> matchConjunctiveConditionsRecursive(Atom newFact, List<Atom> originalConjuncts, Set<Atom> existingFacts) {
        Set<Bindings> finalBindingSets = new HashSet<>();

        // Iterate through each conjunct, treating it as the one potentially matched by newFact
        for (int i = 0; i < originalConjuncts.size(); i++) {
            Atom conjunctMatchedByNewFactPattern = originalConjuncts.get(i);
            List<Bindings> newFactMatches = Matcher.matchAtoms(conjunctMatchedByNewFactPattern, newFact);

            for (Bindings initialBinding : newFactMatches) {
                // `newFact` matched `conjunctMatchedByNewFactPattern` with `initialBinding`.
                // Now, try to satisfy remaining conjuncts.
                List<Atom> remainingConjuncts = new ArrayList<>();
                for(int j=0; j < originalConjuncts.size(); j++) {
                    if (i == j) continue; // Skip the conjunct already matched by newFact
                    remainingConjuncts.add(originalConjuncts.get(j));
                }

                solveRemainingConjuncts(remainingConjuncts, existingFacts, initialBinding, finalBindingSets);
            }
        }
        return new ArrayList<>(finalBindingSets);
    }

    private void solveRemainingConjuncts(List<Atom> remainingConjuncts, Set<Atom> existingFacts,
                                         Bindings currentBindings, Set<Bindings> finalBindingSets) {
        if (remainingConjuncts.isEmpty()) {
            // All conjuncts (including the one initially matched by newFact) have been satisfied.
            finalBindingSets.add(currentBindings);
            return;
        }

        Atom nextConjunctPattern = remainingConjuncts.get(0);
        List<Atom> trulyRemaining = remainingConjuncts.subList(1, remainingConjuncts.size());

        Atom concreteNextConjunctPattern = Matcher.applyBindings(nextConjunctPattern, currentBindings);

        for (Atom factInSpace : existingFacts) {
            // Try to match the concrete pattern (with variables from currentBindings applied)
            // against a fact currently in the space.
            List<Bindings> matches = Matcher.matchAtoms(concreteNextConjunctPattern, factInSpace);
            for (Bindings matchBinding : matches) {
                // currentBindings is the bindings from previous successful matches.
                // matchBinding is the new binding from matching concreteNextConjunctPattern with factInSpace.
                // We need to merge matchBinding into a copy of currentBindings.
                List<Bindings> mergeResults = currentBindings.copy().merge(matchBinding);

                if (!mergeResults.isEmpty()) {
                    for (Bindings newExtendedBindings : mergeResults) {
                        // newExtendedBindings contains currentBindings + matchBinding
                        solveRemainingConjuncts(trulyRemaining, existingFacts, newExtendedBindings, finalBindingSets);
                    }
                } else {
                    // Merge conflict, this path is invalid.
                }
            }
        }
    }
}
