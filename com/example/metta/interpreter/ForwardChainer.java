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
            // Initial call to matchConditions starts with an empty binding.
            List<Bindings> possibleBindings = matchConditions(newFact, conditionsAtom, new Bindings(), existingFacts);

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

    // Entry point for matching rule conditions.
    // newFact is the fact that triggered the rule evaluation.
    // conditionsAtom is the condition part of the rule, e.g., (And A B), (Or C D), (Not E), or just F.
    // currentBindings starts empty for the initial call.
    // existingFacts contains all facts in the space, including newFact.
    private List<Bindings> matchConditions(Atom newFact, Atom conditionsAtom, Bindings currentBindings, Set<Atom> existingFacts) {
        if (conditionsAtom instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) conditionsAtom;
            if (!expr.getChildren().isEmpty()) {
                Atom symbol = expr.getChildren().get(0);
                List<Atom> arguments = expr.getChildren().subList(1, expr.getChildren().size());

                if (MettaSymbols.AND_SYMBOL.equals(symbol)) {
                    if (arguments.isEmpty()) return Collections.emptyList(); // (And) is false
                    // newFact must be involved in satisfying one of the conjuncts.
                    return matchAndConditions(newFact, arguments, currentBindings, existingFacts);
                } else if (MettaSymbols.OR_SYMBOL.equals(symbol)) {
                    if (arguments.isEmpty()) return Collections.emptyList(); // (Or) is false
                    return matchOrConditions(newFact, arguments, currentBindings, existingFacts);
                } else if (MettaSymbols.NOT_SYMBOL.equals(symbol)) {
                    if (arguments.size() != 1) return Collections.emptyList(); // (Not P) - P is one argument
                    return matchNotCondition(newFact, arguments.get(0), currentBindings, existingFacts);
                }
            }
        }
        // Single condition or non-logical expression: must be matched by newFact if this is the top-level call.
        // If it's a recursive call, this single condition could be matched by any existing fact.
        // This specific path handles the case where `conditionsAtom` is a single pattern at the start of matching.
        // It must be matched by newFact.
        Atom concretePattern = Matcher.applyBindings(conditionsAtom, currentBindings);
        List<Bindings> factMatchBindings = Matcher.matchAtoms(concretePattern, newFact);

        return factMatchBindings.stream()
                                .filter(b -> {
                                    // Ensure the binding, when applied to the original pattern (before currentBindings),
                                    // and then combined with currentBindings, is valid.
                                    Bindings merged = currentBindings.copy();
                                    return !merged.merge(b).isEmpty();
                                })
                                .map(b -> {
                                    Bindings finalB = currentBindings.copy();
                                    finalB.merge(b); // We checked for success above.
                                    return finalB;
                                })
                                .collect(Collectors.toList());
    }


    private List<Bindings> matchAndConditions(Atom newFact, List<Atom> conjuncts, Bindings currentBindings, Set<Atom> existingFacts) {
        // This is a refactoring of the old matchConjunctiveConditionsRecursive and solveRemainingConjuncts
        // It needs to ensure newFact is used for at least one conjunct.
        Set<Bindings> finalBindingSets = new HashSet<>();

        for (int i = 0; i < conjuncts.size(); i++) {
            Atom conjunctToMatchWithNewFact = Matcher.applyBindings(conjuncts.get(i), currentBindings);
            List<Bindings> newFactMatches = Matcher.matchAtoms(conjunctToMatchWithNewFact, newFact);

            for (Bindings newFactBinding : newFactMatches) {
                Bindings extendedBindings = currentBindings.copy();
                if (extendedBindings.merge(newFactBinding).isEmpty()) {
                    continue; // Conflict with newFactBinding
                }

                List<Atom> remainingConjuncts = new ArrayList<>();
                for (int j = 0; j < conjuncts.size(); j++) {
                    if (i == j) continue;
                    remainingConjuncts.add(conjuncts.get(j));
                }

                if (remainingConjuncts.isEmpty()) {
                    finalBindingSets.add(extendedBindings);
                } else {
                    // Pass true for newFactUsed, as it has been.
                    solveSubConditions(remainingConjuncts, existingFacts, extendedBindings, finalBindingSets, true);
                }
            }
        }
        return new ArrayList<>(finalBindingSets);
    }

    private List<Bindings> matchOrConditions(Atom newFact, List<Atom> disjuncts, Bindings currentBindings, Set<Atom> existingFacts) {
        Set<Bindings> allSuccessfulBindings = new HashSet<>();
        for (Atom disjunct : disjuncts) {
            // For each disjunct, try to match it. newFact *could* be involved, or it could be other facts.
            // This is tricky: matchConditions was designed for newFact to be the trigger.
            // If a disjunct is (And A B), newFact must satisfy A or B within that disjunct.
            // If a disjunct is P, newFact could satisfy P.

            // Option 1: Try matching the disjunct using newFact explicitly.
            List<Bindings> bindingsUsingNewFact = matchConditions(newFact, disjunct, currentBindings.copy(), existingFacts);
            allSuccessfulBindings.addAll(bindingsUsingNewFact);

            // Option 2: Try matching the disjunct without insisting newFact is the primary matcher for this branch
            // This means the disjunct is satisfied by other facts in existingFacts.
            // This part is complex because the original design heavily relies on newFact.
            // Let's simplify: if newFact is involved, it's covered by matchConditions.
            // If not, then the current OR branch can be satisfied by existing facts if the sub-condition
            // can be satisfied by them.
            // This might require a version of solveSubConditions that doesn't assume newFact involvement for the *first* item.
            // For now, let's assume matchConditions is flexible enough if currentBindings is passed correctly.
            // The key is that `matchConditions` for a single atom will try to match it against newFact.
            // If `disjunct` is an AND, `matchAndConditions` will ensure newFact is used there.

        }
        return new ArrayList<>(allSuccessfulBindings);
    }

    private List<Bindings> matchNotCondition(Atom newFact, Atom conditionToNegate, Bindings currentBindings, Set<Atom> existingFacts) {
        // We need to check if the conditionToNegate, when currentBindings are applied,
        // can be matched by any fact in existingFacts.
        // The findMatchesForPattern method will apply currentBindings to conditionToNegate.
        // newFactAlreadyUsedInBranch is false because NOT doesn't "use" newFact positively.
        List<Bindings> potentialMatches = findMatchesForPattern(conditionToNegate, existingFacts, currentBindings, false);

        if (potentialMatches.isEmpty()) {
            // No match found for conditionToNegate (after applying currentBindings) in existingFacts.
            // So, (Not conditionToNegate) is true. Return current bindings as they are.
            return Collections.singletonList(currentBindings.copy());
        } else {
            // Match found for P, so (Not P) is false.
            return Collections.emptyList();
        }
    }

    // General recursive solver for a list of conditions (conjuncts or parts of other structures)
    // newFactUsed indicates if newFact has already been "used" to satisfy a part of the rule.
    private void solveSubConditions(List<Atom> conditions, Set<Atom> existingFacts,
                                    Bindings currentBindings, Set<Bindings> finalBindingSets, boolean newFactUsed) {
        if (conditions.isEmpty()) {
            finalBindingSets.add(currentBindings);
            return;
        }

        Atom nextConditionPattern = conditions.get(0);
        List<Atom> remainingConditions = conditions.subList(1, conditions.size());
        Atom concreteNextConditionPattern = Matcher.applyBindings(nextConditionPattern, currentBindings);

        // If newFact hasn't been used, it could potentially match this nextConditionPattern.
        // This logic becomes complex quickly. The original design was that newFact matches *one* conjunct in an AND.
        // For now, this helper assumes that if newFact needs to be involved, it was handled by the caller
        // (e.g., matchAndConditions). This function matches remaining conditions against existingFacts.

        List<Bindings> potentialMatches = findMatchesForPattern(concreteNextConditionPattern, existingFacts, currentBindings, newFactUsed);
        for (Bindings newMatchBinding : potentialMatches) {
            Bindings extendedBindings = currentBindings.copy();
            if (!extendedBindings.merge(newMatchBinding).isEmpty()) {
                 solveSubConditions(remainingConditions, existingFacts, extendedBindings, finalBindingSets, newFactUsed);
            }
        }
    }

    // Helper to find matches for a single pattern against existingFacts.
    // `patternToMatch` is the raw pattern (may contain variables).
    // `bindingsContext` provides the current variable bindings to apply to `patternToMatch` before matching.
    // Returns a list of new bindings (for variables in `patternToMatch`) that are consistent with `bindingsContext`.
    private List<Bindings> findMatchesForPattern(Atom patternToMatch, Set<Atom> factsToSearch, Bindings bindingsContext, boolean newFactAlreadyUsedInBranch) {
        Set<Bindings> successfulBindings = new HashSet<>();
        Atom concretePattern = Matcher.applyBindings(patternToMatch, bindingsContext);

        for (Atom fact : factsToSearch) {
            // Try to match the concrete pattern (with variables from bindingsContext applied) against a fact.
            // Matcher.matchAtoms returns bindings relative to concretePattern and fact.
            List<Bindings> directMatches = Matcher.matchAtoms(concretePattern, fact);
            for (Bindings directMatch : directMatches) {
                // `directMatch` contains bindings for variables that were still in `concretePattern`.
                // These bindings should be valid on their own.
                // The `concretePattern` was already made consistent with `bindingsContext`.
                // So, any `directMatch` found is a valid extension of `bindingsContext`.
                // The crucial part is that `Matcher.matchAtoms` doesn't know about `bindingsContext`,
                // it just sees `concretePattern`. If `concretePattern` had variables that were
                // already bound in `bindingsContext`, they were replaced by their values.
                // If `concretePattern` still has variables, `directMatch` will bind them.

                // We need to ensure the returned bindings can be merged with the original context,
                // though `concretePattern` should have already handled this.
                // This check is more of a sanity check or if `patternToMatch` itself had variables
                // that `bindingsContext` also defined, which `Matcher.applyBindings` should resolve.
                Bindings checkMerge = bindingsContext.copy();
                if (!checkMerge.merge(directMatch).isEmpty()) {
                    successfulBindings.add(directMatch); // Store the bindings from this match
                }
            }
        }
        return new ArrayList<>(successfulBindings);
    }


    // This is the old matchConjunctiveConditionsRecursive, to be replaced or refactored.
    // For now, keeping its signature if other parts rely on it, but its logic will be in matchAndConditions.
    private List<Bindings> matchConjunctiveConditionsRecursive(Atom newFact, List<Atom> originalConjuncts, Set<Atom> existingFacts) {
        // This will delegate to matchAndConditions with an initial empty binding.
        return matchAndConditions(newFact, originalConjuncts, new Bindings(), existingFacts);
    }

    // This is the old solveRemainingConjuncts, to be replaced or refactored.
    private void solveRemainingConjuncts(List<Atom> remainingConjuncts, Set<Atom> existingFacts,
                                         Bindings currentBindings, Set<Bindings> finalBindingSets) {
        // This will delegate to the new solveSubConditions.
        // The boolean newFactUsed needs to be determined. If this is called from the context
        // of matchAndConditions, newFact has been used.
        solveSubConditions(remainingConjuncts, existingFacts, currentBindings, finalBindingSets, true); // newFactUsed is true here
    }
}
