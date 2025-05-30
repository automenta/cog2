package com.example.metta.space;

import com.example.metta.atom.Atom;
import com.example.metta.atom.ExpressionAtom;
import com.example.metta.atom.MettaSymbols;
import com.example.metta.atom.VariableAtom;
import com.example.metta.interpreter.ForwardChainer;
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
        return this.atoms.stream()
            .flatMap(dataAtom -> Matcher.matchAtoms(pattern, dataAtom).stream())
            .collect(Collectors.toList());
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
}
