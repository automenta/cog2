package org.opencog.hyperon.space;

import org.opencog.hyperon.atoms.*;

import java.util.*;
import java.util.stream.Collectors;

public class AtomSpace {
    private final Set<Atom> atoms;

    public AtomSpace() {
        // Using HashSet for simplicity first.
        // For thread-safety, ConcurrentHashMap.newKeySet() could be used.
        this.atoms = new HashSet<>();
    }

    public void addAtom(Atom atom) {
        if (atom == null) {
            throw new IllegalArgumentException("Cannot add a null atom to the space.");
        }
        // Ensure atom is valid (e.g. ExpressionAtom children are not null, etc.)
        // This might be better done within Atom constructors or a validation utility.
        if (atom instanceof ExpressionAtom) {
            for (Atom child : ((ExpressionAtom) atom).getChildren()) {
                if (child == null) {
                    throw new IllegalArgumentException("ExpressionAtom cannot have null children.");
                }
            }
        }
        this.atoms.add(atom);
    }

    public Set<Atom> getAtoms() {
        return Collections.unmodifiableSet(atoms);
    }

    public List<Bindings> query(Atom pattern) {
        List<Bindings> resultBindings = new ArrayList<>();
        for (Atom atom : this.atoms) {
            Bindings bindings = match(atom, pattern, new Bindings());
            if (bindings != null) {
                resultBindings.add(bindings);
            }
        }
        return resultBindings;
    }

    private Bindings match(Atom dataAtom, Atom patternAtom, Bindings currentBindings) {
        if (patternAtom instanceof VariableAtom) {
            VariableAtom varPattern = (VariableAtom) patternAtom;
            if (currentBindings.isBound(varPattern)) {
                // If variable is already bound, the data must match the bound value
                return match(dataAtom, currentBindings.getValue(varPattern), currentBindings);
            } else {
                // Variable not bound, try to bind it to the data atom
                try {
                    return currentBindings.addBinding(varPattern, dataAtom);
                } catch (IllegalStateException e) {
                    // This can happen if varPattern was already in currentBindings
                    // (e.g. through an earlier step in matching an expression)
                    // and dataAtom is different. This path should ideally be covered
                    // by the isBound check above, but addBinding has its own check.
                    return null;
                }
            }
        }

        if (dataAtom.getType() != patternAtom.getType()) {
            return null; // Types must match
        }

        if (patternAtom instanceof SymbolAtom || patternAtom instanceof GroundedAtom) {
            return patternAtom.equals(dataAtom) ? currentBindings : null;
        }

        if (patternAtom instanceof ExpressionAtom) {
            if (!(dataAtom instanceof ExpressionAtom)) {
                return null; // Should have been caught by type check, but defensive
            }
            ExpressionAtom exprPattern = (ExpressionAtom) patternAtom;
            ExpressionAtom exprData = (ExpressionAtom) dataAtom;

            List<Atom> patternChildren = exprPattern.getChildren();
            List<Atom> dataChildren = exprData.getChildren();

            if (patternChildren.size() != dataChildren.size()) {
                return null; // Expressions must have the same number of children
            }

            Bindings accumulatedBindings = currentBindings;
            for (int i = 0; i < patternChildren.size(); i++) {
                accumulatedBindings = match(dataChildren.get(i), patternChildren.get(i), accumulatedBindings);
                if (accumulatedBindings == null) {
                    return null; // Child match failed
                }
            }
            return accumulatedBindings;
        }

        // Should not be reached if all AtomTypes are handled
        return null;
    }

    // Helper for debugging or display
    @Override
    public String toString() {
        return "AtomSpace{" +
                "atoms=" + atoms.stream().map(Atom::toString).collect(Collectors.joining(", ")) +
                '}';
    }
}
