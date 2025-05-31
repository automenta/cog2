package com.example.metta.space;

import com.example.metta.atom.Atom;
import com.example.metta.atom.ExpressionAtom;
import com.example.metta.atom.LinkAtom; // Added import
import com.example.metta.atom.MettaSymbols;
import com.example.metta.atom.VariableAtom;
import com.example.metta.interpreter.ForwardChainer;
import com.example.metta.interpreter.Pair; // Added import
import com.example.metta.matcher.Matcher;
import com.example.metta.types.Bindings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections; // Added for Collections.emptyList in narrowVariables
import java.util.Queue;
import java.util.LinkedList;

public class GroundingSpace implements SpaceWriter, SpaceReader { // Assuming SpaceReader is implemented or its methods are present

    private final Set<Atom> atoms;
    private final ForwardChainer forwardChainer = new ForwardChainer();
    private boolean enableForwardChaining = false;
    private static final int MAX_PATH_STAR_DEPTH = 10;

    public GroundingSpace() {
        this.atoms = new HashSet<>();
    }

    /**
     * Enables or disables the automatic forward chaining mechanism.
     * When enabled, adding facts to the space may trigger rules and derive new facts.
     * @param enable true to enable forward chaining, false to disable.
     */
    public void setEnableForwardChaining(boolean enable) {
        this.enableForwardChaining = enable;
    }

    @Override
    public void add(Atom atom) {
        // Add the initial atom to the main set of atoms.
        // The boolean `isNewToSpace` indicates if this atom was actually new.
        // This information isn't strictly used to decide IF chaining occurs,
        // but it's often useful. Chaining will occur if enableForwardChaining is true,
        // using 'atom' as the initial trigger.
        boolean isNewToSpace = this.atoms.add(atom);

        if (enableForwardChaining) {
            Queue<Atom> processingQueue = new LinkedList<>();
            // Offer the atom that was just 'added'. It's the primary trigger for this cycle.
            processingQueue.offer(atom);

            // This set tracks atoms that have been added to the queue during this specific
            // invocation of add(), to prevent redundant processing within the same cascade.
            Set<Atom> atomsQueuedForThisCascade = new HashSet<>();
            atomsQueuedForThisCascade.add(atom);

            while (!processingQueue.isEmpty()) {
                Atom currentFactToProcess = processingQueue.poll();

                // Create a snapshot of all atoms currently in the space.
                // This ensures the chainer sees a consistent state for its reasoning step.
                Set<Atom> allAtomsSnapshot = new HashSet<>(this.atoms);

                // The forwardChainer's trigger method uses currentFactToProcess as the 'new fact'
                // (the one that potentially completes a rule) and allAtomsSnapshot as the
                // context of all other existing facts.
                Set<Atom> newlyDerivedConclusions = forwardChainer.trigger(this, currentFactToProcess, allAtomsSnapshot);

                for (Atom conclusion : newlyDerivedConclusions) {
                    // `newlyDerivedConclusions` are already confirmed by `trigger`
                    // not to be in `allAtomsSnapshot` that was passed to it (meaning they are new relative to that snapshot).
                    // Now, we add it to the main `this.atoms` set in GroundingSpace.
                    if (this.atoms.add(conclusion)) { // If truly new to the main atom set
                        // And if we haven't already queued it up during this current `add` cascade
                        if (atomsQueuedForThisCascade.add(conclusion)) {
                            processingQueue.offer(conclusion);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean remove(Atom atom) {
        return this.atoms.remove(atom);
    }

    @Override
    public boolean replace(Atom from, Atom to) {
        if (this.atoms.remove(from)) {
            this.atoms.add(to);
            return true;
        }
        return false;
    }

    @Override
    public List<Atom> getAtoms() {
        return new ArrayList<>(this.atoms);
    }

    @Override
    public boolean contains(Atom atom) {
        return this.atoms.contains(atom);
    }

    private void collectVariables(Atom pattern, Set<VariableAtom> vars) {
        if (pattern instanceof VariableAtom) {
            vars.add((VariableAtom) pattern);
        } else if (pattern instanceof ExpressionAtom) {
            for (Atom child : ((ExpressionAtom) pattern).getChildren()) {
                collectVariables(child, vars);
            }
        }
    }
    
    private Set<VariableAtom> getVariablesInPattern(Atom pattern) {
        Set<VariableAtom> vars = new HashSet<>();
        collectVariables(pattern, vars);
        return vars;
    }

    private Bindings narrowVariables(Bindings bindings, Set<VariableAtom> relevantVars) {
        if (relevantVars.isEmpty()) {
            return new Bindings(); // No relevant vars, result is an empty binding set
        }
        Bindings narrowed = new Bindings();
        for (VariableAtom var : relevantVars) {
            Atom value = bindings.resolve(var);
            if (value != null) {
                if (value instanceof VariableAtom && relevantVars.contains(value)) {
                    narrowed.addVariableEquality(var, (VariableAtom)value);
                } else {
                    // If value is a concrete atom, or a variable not in relevantVars (which means it's effectively concrete for this scope)
                    narrowed.addValueBinding(var, value);
                }
            }
            // If value is null, the variable remains unbound in the narrowed set, which is correct.
        }
        
        // Ensure that equalities between relevant variables that might not have explicit value bindings
        // are preserved. E.g. if $X=$Y and both are relevant but unbound.
        for (VariableAtom var1 : relevantVars) {
            for (VariableAtom var2 : relevantVars) {
                if (var1.equals(var2)) continue;
                // Check if var1 and var2 are equivalent in the original bindings
                VariableAtom root1 = bindings.find(var1);
                VariableAtom root2 = bindings.find(var2);
                if (root1.equals(root2)) {
                    // If they are equivalent, ensure this equality is in narrowed.
                    // addVariableEquality is idempotent and handles existing compatible bindings.
                    narrowed.addVariableEquality(var1, var2);
                }
            }
        }
        return narrowed;
    }

    @Override
    public List<Bindings> query(Atom queryPattern) {
        Set<VariableAtom> relevantVars = getVariablesInPattern(queryPattern);
        List<Bindings> resultBindings;

        if (queryPattern instanceof ExpressionAtom) {
            ExpressionAtom exprPattern = (ExpressionAtom) queryPattern;
            List<Atom> children = exprPattern.getChildren();
            if (!children.isEmpty() && children.get(0).equals(MettaSymbols.COMMA_SYMBOL)) {
                if (children.size() == 1) { // Just (,)
                    return Collections.singletonList(narrowVariables(new Bindings(), relevantVars));
                }
                resultBindings = executeConjunctiveQuery(children.subList(1, children.size()), relevantVars);
            } else if (!children.isEmpty() && children.get(0).equals(MettaSymbols.TRAVERSE_SYMBOL)) {
                resultBindings = executeTraversalQuery(exprPattern); // Call the new method
            } else {
                resultBindings = matchAgainstSpace(queryPattern);
            }
        } else {
            resultBindings = matchAgainstSpace(queryPattern);
        }

        return resultBindings.stream()
                .map(b -> narrowVariables(b, relevantVars))
                .filter(b -> !b.hasLoop()) 
                .distinct() 
                .collect(Collectors.toList());
    }

    private List<Bindings> matchAgainstSpace(Atom pattern) {
        System.err.println("DEBUG_MAS: Called with pattern: " + pattern);
        List<Bindings> allResults = new ArrayList<>();
        for (Atom dataAtom : this.atoms) {
            Atom effectiveDataAtom = dataAtom;
            boolean isLink = false;
            if (dataAtom instanceof com.example.metta.atom.LinkAtom) {
                effectiveDataAtom = ((com.example.metta.atom.LinkAtom)dataAtom).toExpressionAtom();
                isLink = true;
            }
            System.err.println("DEBUG_MAS:   DataAtom: " + dataAtom + (isLink ? " (as Link, effective: " + effectiveDataAtom + ")" : ""));

            List<Bindings> matchResults = Matcher.matchAtoms(pattern, effectiveDataAtom);

            if (!matchResults.isEmpty()) {
                System.err.println("DEBUG_MAS:     MATCHED! Pattern: " + pattern + " with EffectiveDataAtom: " + effectiveDataAtom + " -> Bindings: " + matchResults);
                allResults.addAll(matchResults);
            } else {
                System.err.println("DEBUG_MAS:     NO MATCH. Pattern: " + pattern + " with EffectiveDataAtom: " + effectiveDataAtom);
            }
        }
        System.err.println("DEBUG_MAS: Returning total " + allResults.size() + " bindings for pattern: " + pattern);
        return allResults.stream().distinct().collect(Collectors.toList()); // Keep distinct for now
    }

    private List<Bindings> executeConjunctiveQuery(List<Atom> subQueries, Set<VariableAtom> relevantVars) {
        List<Bindings> accumulatedBindings = new ArrayList<>();
        accumulatedBindings.add(new Bindings()); 

        for (Atom subQueryAtom : subQueries) {
            if (accumulatedBindings.isEmpty()) break; 

            accumulatedBindings = accumulatedBindings.stream()
                .flatMap(currentBinding -> {
                    Atom concreteSubQuery = Matcher.applyBindings(subQueryAtom, currentBinding);
                    return this.atoms.stream()
                        .flatMap(dataAtom -> Matcher.matchAtoms(concreteSubQuery, dataAtom).stream())
                        .flatMap(subBinding -> {
                            Bindings merged = currentBinding.copy();
                            // merge returns a list (usually 0 or 1 element for non-disjunctive merges)
                            return merged.merge(subBinding).stream(); 
                        });
                })
                .distinct() // Keep distinct bindings at each step
                .collect(Collectors.toList());
        }
        return accumulatedBindings;
    }

    @Override
    public List<Atom> subst(Atom pattern, Atom template) {
        return query(pattern).stream()
            .map(bindings -> Matcher.applyBindings(template, bindings))
            .distinct()
            .collect(Collectors.toList());
    }

    private List<Bindings> executeTraversalQuery(ExpressionAtom queryExpr) {
        if (queryExpr.getChildren().size() != 4) {
            System.err.println("Warning: Traverse query expects 3 arguments (start, path, end), but got: " + (queryExpr.getChildren().size() -1));
            return Collections.emptyList();
        }

        Atom startNodeConstraint = queryExpr.getChildren().get(1);
        Atom pathPattern = queryExpr.getChildren().get(2);
        Atom endNodeConstraint = queryExpr.getChildren().get(3);

        List<Bindings> finalResults = new ArrayList<>();

        if (pathPattern instanceof ExpressionAtom &&
            !((ExpressionAtom) pathPattern).getChildren().isEmpty() &&
            ((ExpressionAtom) pathPattern).getChildren().get(0).equals(MettaSymbols.PATH_SYMBOL)) {
            // Sequential Path Logic
            ExpressionAtom pathSequenceExpr = (ExpressionAtom) pathPattern;
            List<Atom> linkPatternsInSequence = pathSequenceExpr.getChildren().subList(1, pathSequenceExpr.getChildren().size());

            if (linkPatternsInSequence.isEmpty()) {
                return Collections.emptyList();
            }

            List<Pair<Atom, Bindings>> currentIterationResults = new ArrayList<>();

            // Initial Step: Populate currentIterationResults based on startNodeConstraint
            if (startNodeConstraint instanceof VariableAtom) {
                for (Atom atom : this.atoms) { // Consider all atoms as potential starts
                    currentIterationResults.add(new Pair<>(atom, new Bindings()));
                }
            } else {
                currentIterationResults.add(new Pair<>(startNodeConstraint, new Bindings()));
            }

            // Iterate through each link pattern in the sequence
            for (Atom currentLinkPatternAtom : linkPatternsInSequence) {
                if (!(currentLinkPatternAtom instanceof ExpressionAtom)) {
                    return Collections.emptyList(); // Invalid path component
                }
                ExpressionAtom currentLinkPatternExpr = (ExpressionAtom) currentLinkPatternAtom;
                List<Pair<Atom, Bindings>> nextIterationResults = new ArrayList<>();

                Atom patternLinkType = currentLinkPatternExpr.getChildren().get(0);
                List<Atom> patternTargets = currentLinkPatternExpr.getChildren().subList(1, currentLinkPatternExpr.getChildren().size());
                if (patternTargets.isEmpty()) continue; // Or handle as error: path link must have targets

                Atom patternStartTargetComponent = patternTargets.get(0);
                Atom patternEndTargetComponent = patternTargets.size() == 1 ? patternStartTargetComponent : patternTargets.get(patternTargets.size() - 1);

                for (Pair<Atom, Bindings> prevStepPair : currentIterationResults) {
                    Atom prevStepEndNode = prevStepPair.getLeft(); // This is the node to start from for this step
                    Bindings prevStepBindings = prevStepPair.getRight();

                    for (Atom atomInSpace : this.atoms) {
                        if (!(atomInSpace instanceof LinkAtom)) continue;
                        LinkAtom actualLink = (LinkAtom) atomInSpace;

                        if (!actualLink.getLinkType().equals(patternLinkType) ||
                            actualLink.getTargets().size() != patternTargets.size()) {
                            continue;
                        }

                        ExpressionAtom actualLinkAsExpr = actualLink.toExpressionAtom();
                        List<Bindings> structuralMatches = Matcher.matchAtoms(currentLinkPatternExpr, actualLinkAsExpr);

                        for (Bindings currentLinkBinding : structuralMatches) {
                            // `currentLinkBinding` maps vars in `currentLinkPatternExpr` to parts of `actualLinkAsExpr`
                            Bindings trialBindings = prevStepBindings.copy();
                            List<Bindings> mergedStructural = trialBindings.merge(currentLinkBinding);
                            if (mergedStructural.isEmpty()) continue;
                            trialBindings = mergedStructural.get(0);

                            // Check if prevStepEndNode matches the start of this actualLink, using trialBindings
                            Atom boundPrevStepEndNode = Matcher.applyBindings(prevStepEndNode, trialBindings);
                            Atom boundPatternStartTarget = Matcher.applyBindings(patternStartTargetComponent, trialBindings);

                            List<Bindings> startConstraintMatches = Matcher.matchAtoms(boundPrevStepEndNode, boundPatternStartTarget);

                            for (Bindings scm : startConstraintMatches) {
                                Bindings iterationBinding = trialBindings.copy();
                                List<Bindings> mergedScm = iterationBinding.merge(scm);
                                if (mergedScm.isEmpty()) continue;
                                iterationBinding = mergedScm.get(0);

                                Atom nextNodeForPath = Matcher.applyBindings(patternEndTargetComponent, iterationBinding);
                                nextIterationResults.add(new Pair<>(nextNodeForPath, iterationBinding));
                            }
                        }
                    }
                }
                currentIterationResults = nextIterationResults.stream().distinct().collect(Collectors.toList());
                if (currentIterationResults.isEmpty()) break; // Path broken
            }

            // Final Step: Filter by endNodeConstraint
            List<Bindings> pathFinalResults = new ArrayList<>(); // Renamed to avoid conflict with outer finalResults
            for (Pair<Atom, Bindings> pair : currentIterationResults) {
                Atom lastNodeOfPath = pair.getLeft();
                Bindings pathBindings = pair.getRight();

                Atom boundEndNodeConstraint = Matcher.applyBindings(endNodeConstraint, pathBindings);
                Atom boundLastNodeOfPath = Matcher.applyBindings(lastNodeOfPath, pathBindings); // Should be mostly concrete

                List<Bindings> endMatches = Matcher.matchAtoms(boundEndNodeConstraint, boundLastNodeOfPath);
                for (Bindings em : endMatches) {
                    Bindings finalB = pathBindings.copy();
                    List<Bindings> mergedEm = finalB.merge(em);
                    if(!mergedEm.isEmpty()){
                        pathFinalResults.add(mergedEm.get(0));
                    }
                }
            }
            return pathFinalResults.stream().distinct().collect(Collectors.toList()); // Return pathFinalResults here
        } else if (pathPattern instanceof ExpressionAtom &&
              !((ExpressionAtom) pathPattern).getChildren().isEmpty() &&
              ((ExpressionAtom) pathPattern).getChildren().get(0).equals(MettaSymbols.PATH_STAR_SYMBOL)) {
            // New PathStar logic
            ExpressionAtom pathStarExpression = (ExpressionAtom) pathPattern; // pathPattern is (PathStar <actual_link_pattern>)
            if (pathStarExpression.getChildren().size() != 2) {
                System.err.println("Warning: PathStar expects one argument (the link pattern). Pattern: " + pathStarExpression);
                return Collections.emptyList();
            }
            Atom actualLinkPattern = pathStarExpression.getChildren().get(1);
            if (!(actualLinkPattern instanceof ExpressionAtom)) {
                System.err.println("Warning: Link pattern inside PathStar must be an Expression. Pattern: " + actualLinkPattern);
                return Collections.emptyList();
            }
            Set<VariableAtom> overallRelevantVars = getVariablesInPattern(queryExpr); // queryExpr is the full (traverse ...)
            return executePathStarTraversal(startNodeConstraint, (ExpressionAtom) actualLinkPattern, endNodeConstraint, overallRelevantVars);
        } else if (pathPattern instanceof ExpressionAtom) {
            // Single link traversal
            ExpressionAtom singleLinkPatternExpr = (ExpressionAtom) pathPattern;

            // potentialLinkMatches will contain bindings for variables within singleLinkPatternExpr
            List<Bindings> potentialLinkMatches = matchAgainstSpace(singleLinkPatternExpr); // This will now have debug prints

            if (singleLinkPatternExpr.getChildren().size() < 2) {
                System.err.println("Warning: Single link pattern in Traverse must have at least a type and one target. Pattern: " + singleLinkPatternExpr);
                return Collections.emptyList();
            }

            List<Atom> patternTargets = singleLinkPatternExpr.getChildren().subList(1, singleLinkPatternExpr.getChildren().size());
            Atom patternStartTarget = patternTargets.get(0);
            Atom patternEndTarget = patternTargets.size() == 1 ? patternStartTarget : patternTargets.get(patternTargets.size() - 1);

            // The logic from Turn 45 for constraint checking (restored):
            for (Bindings linkBinding : potentialLinkMatches) {
                // Initial binding from matching the link pattern itself with a LinkAtom in space
                Bindings baseBinding = linkBinding.copy();

                Atom resolvedPatternStartTarget = Matcher.applyBindings(patternStartTarget, baseBinding);
                Atom resolvedPatternEndTarget = Matcher.applyBindings(patternEndTarget, baseBinding);

                // --- Start Constraint Check ---
                // Match the query's startNodeConstraint against what the link's start resolved to.
                List<Bindings> startConstraintMatches = Matcher.matchAtoms(startNodeConstraint, resolvedPatternStartTarget);

                if (startConstraintMatches.isEmpty()) {
                    // If no way to match startNodeConstraint with resolvedPatternStartTarget, this linkBinding path is invalid.
                    continue;
                }

                // Attempt to merge the original linkBinding with the result of the start constraint match.
                Bindings tempBindingsForStartMerge = linkBinding.copy();
                List<Bindings> bindingsAfterStartConstraintApplied = tempBindingsForStartMerge.merge(startConstraintMatches.get(0));

                if (bindingsAfterStartConstraintApplied.isEmpty()) {
                    continue;
                }
                Bindings bindingWithStartConstraint = bindingsAfterStartConstraintApplied.get(0);

                // --- End Constraint Check ---
                Atom resolvedPatternEndTargetAfterStartConstraint = Matcher.applyBindings(patternEndTarget, bindingWithStartConstraint);
                List<Bindings> endConstraintMatches = Matcher.matchAtoms(endNodeConstraint, resolvedPatternEndTargetAfterStartConstraint);

                if (endConstraintMatches.isEmpty()) {
                    continue;
                }
                List<Bindings> bindingWithEndConstraintApplied = bindingWithStartConstraint.copy().merge(endConstraintMatches.get(0));

                if (bindingWithEndConstraintApplied.isEmpty()) {
                    continue;
                }

                finalResults.add(bindingWithEndConstraintApplied.get(0));
            }
        } else {
            System.err.println("Warning: Path pattern in Traverse must be an ExpressionAtom. Found: " + pathPattern);
            return Collections.emptyList();
        }
        return finalResults.stream().distinct().collect(Collectors.toList());
    }

    private List<Bindings> executePathStarTraversal(
        Atom startNodeConstraint,
        ExpressionAtom linkPattern, // This is the <actual_link_pattern> like (L $X $Y)
        Atom endNodeConstraint,
        Set<VariableAtom> overallRelevantVars) {

        List<Atom> linkPatternChildren = linkPattern.getChildren();
        if (linkPatternChildren.size() < 2) {
            System.err.println("Warning: PathStar link pattern must have at least two children (e.g., Type FromVar ToVar). Pattern: " + linkPattern);
            return Collections.emptyList();
        }
        Atom linkFromVarPlaceholder = linkPatternChildren.get(1);
        Atom linkToVarPlaceholder = linkPatternChildren.get(linkPatternChildren.size() - 1);
        if (!(linkFromVarPlaceholder instanceof VariableAtom) || !(linkToVarPlaceholder instanceof VariableAtom)) {
            System.err.println("Warning: PathStar link pattern's assumed from/to placeholders (child 1 and last child) must be variables. Pattern: " + linkPattern);
            return Collections.emptyList();
        }

        List<Bindings> finalResults = new ArrayList<>();
        Queue<Pair<Atom, Pair<Bindings, Integer>>> queue = new LinkedList<>();
        Set<Pair<Atom, Integer>> visitedWithDepth = new HashSet<>();

        Atom resolvedStartNode = Matcher.applyBindings(startNodeConstraint, new Bindings());

        if (resolvedStartNode instanceof VariableAtom) {
            for (Atom potentialStartAtom : this.atoms) {
                Bindings initialPathBindings = new Bindings();
                if (initialPathBindings.addValueBinding((VariableAtom)resolvedStartNode, potentialStartAtom)) {
                    if (!visitedWithDepth.contains(new Pair<>(potentialStartAtom, 0))) {
                        queue.offer(new Pair<>(potentialStartAtom, new Pair<>(initialPathBindings, 0)));
                        visitedWithDepth.add(new Pair<>(potentialStartAtom, 0));
                    }
                }
            }
        } else {
            if (!visitedWithDepth.contains(new Pair<>(resolvedStartNode, 0))) {
                queue.offer(new Pair<>(resolvedStartNode, new Pair<>(new Bindings(), 0)));
                visitedWithDepth.add(new Pair<>(resolvedStartNode, 0));
            }
        }

        while (!queue.isEmpty()) {
            Pair<Atom, Pair<Bindings, Integer>> currentQueueEntry = queue.poll();
            Atom currentNode = currentQueueEntry.getLeft();
            Bindings currentPathBindings = currentQueueEntry.getRight().getLeft();
            int currentDepth = currentQueueEntry.getRight().getRight();

            if (currentDepth > MAX_PATH_STAR_DEPTH) {
                continue;
            }

            Atom concreteEndNodeConstraint = Matcher.applyBindings(endNodeConstraint, currentPathBindings);
            List<Bindings> endMatchAttempt = Matcher.matchAtoms(concreteEndNodeConstraint, currentNode);

            for (Bindings endMatchSpecificBindings : endMatchAttempt) {
                Bindings fullPathBindings = currentPathBindings.copy();
                List<Bindings> merged = fullPathBindings.merge(endMatchSpecificBindings);
                if (!merged.isEmpty()) {
                    finalResults.add(merged.get(0));
                }
            }

            if (currentDepth == MAX_PATH_STAR_DEPTH) {
                continue;
            }

            Bindings bindingsForStepQuery = currentPathBindings.copy();
            Atom resolvedLinkFromVarInPattern = Matcher.applyBindings(linkFromVarPlaceholder, bindingsForStepQuery);

            if (resolvedLinkFromVarInPattern instanceof VariableAtom) {
                if (!bindingsForStepQuery.addValueBinding((VariableAtom)resolvedLinkFromVarInPattern, currentNode)) {
                    continue;
                }
            } else if (!resolvedLinkFromVarInPattern.equals(currentNode)) {
                continue;
            }

            Atom stepQueryAtom = Matcher.applyBindings(linkPattern, bindingsForStepQuery);
            if (stepQueryAtom instanceof VariableAtom || stepQueryAtom == null) {
                continue;
            }

            List<Bindings> stepMatchResults = matchAgainstSpace(stepQueryAtom);

            for (Bindings stepSpecificBindings : stepMatchResults) {
                Bindings nextNodePathBindings = bindingsForStepQuery.copy();
                List<Bindings> mergedStep = nextNodePathBindings.merge(stepSpecificBindings);

                if (mergedStep.isEmpty()) {
                    continue;
                }
                nextNodePathBindings = mergedStep.get(0);

                Atom nextNodeConcrete = Matcher.applyBindings(linkToVarPlaceholder, nextNodePathBindings);

                if (nextNodeConcrete == null || nextNodeConcrete instanceof VariableAtom) {
                    continue;
                }

                if (!visitedWithDepth.contains(new Pair<>(nextNodeConcrete, currentDepth + 1))) {
                    queue.offer(new Pair<>(nextNodeConcrete, new Pair<>(nextNodePathBindings, currentDepth + 1)));
                    visitedWithDepth.add(new Pair<>(nextNodeConcrete, currentDepth + 1));
                }
            }
        }

        return finalResults.stream()
            .map(b -> narrowVariables(b, overallRelevantVars))
            .filter(b -> !b.hasLoop())
            .distinct()
            .collect(Collectors.toList());
    }
}
