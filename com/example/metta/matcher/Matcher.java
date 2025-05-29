package com.example.metta.matcher;

import com.example.metta.atom.*;
import com.example.metta.types.Bindings;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Objects;

public final class Matcher {

    private Matcher() {
    }

    public static List<Bindings> matchAtoms(Atom pattern, Atom target) {
        Objects.requireNonNull(pattern, "pattern cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        return matchAtomsRecursive(pattern, target, new Bindings());
    }

    private static List<Bindings> matchAtomsRecursive(Atom pattern, Atom target, Bindings currentBindings) {
        List<Bindings> result = new ArrayList<>();

        Atom resolvedPattern = pattern;
        if (pattern instanceof VariableAtom) {
            Atom p = currentBindings.resolve((VariableAtom) pattern);
            if (p != null) resolvedPattern = p; // p could be the variable itself if part of a loop or unresolved
        }

        Atom resolvedTarget = target;
        if (target instanceof VariableAtom) {
            Atom t = currentBindings.resolve((VariableAtom) target);
            if (t != null) resolvedTarget = t;
        }

        if (resolvedPattern instanceof VariableAtom) {
            Bindings newBindings = currentBindings.copy();
            if (newBindings.addValueBinding((VariableAtom) resolvedPattern, resolvedTarget)) { // resolvedTarget could be a var or concrete
                if (!newBindings.hasLoop()) {
                    result.add(newBindings);
                }
            }
        } else if (resolvedTarget instanceof VariableAtom) {
            Bindings newBindings = currentBindings.copy();
            if (newBindings.addValueBinding((VariableAtom) resolvedTarget, resolvedPattern)) {
                 if (!newBindings.hasLoop()) {
                    result.add(newBindings);
                }
            }
        } else if (resolvedPattern instanceof SymbolAtom && resolvedTarget instanceof SymbolAtom) {
            if (resolvedPattern.equals(resolvedTarget)) {
                result.add(currentBindings.copy()); 
            }
        } else if (resolvedPattern instanceof ExpressionAtom && resolvedTarget instanceof ExpressionAtom) {
            ExpressionAtom exprPattern = (ExpressionAtom) resolvedPattern;
            ExpressionAtom exprTarget = (ExpressionAtom) resolvedTarget;

            if (exprPattern.getChildren().size() == exprTarget.getChildren().size()) {
                List<Bindings> currentLevelBindings = Collections.singletonList(currentBindings.copy());

                for (int i = 0; i < exprPattern.getChildren().size(); i++) {
                    Atom childPattern = exprPattern.getChildren().get(i);
                    Atom childTarget = exprTarget.getChildren().get(i);
                    
                    currentLevelBindings = currentLevelBindings.stream()
                        .flatMap(prevBindings -> matchAtomsRecursive(childPattern, childTarget, prevBindings).stream())
                        .collect(Collectors.toList()); // distinct() will be applied at the end of method
                    
                    if (currentLevelBindings.isEmpty()) {
                        break; 
                    }
                }
                result.addAll(currentLevelBindings);
            }
        } else if (resolvedPattern instanceof GroundedAtom) {
            GroundedAtom groundedPattern = (GroundedAtom) resolvedPattern;
            List<Bindings> groundedMatchResults = groundedPattern.match(resolvedTarget); 

            for (Bindings groundedBindings : groundedMatchResults) {
                Bindings merged = currentBindings.copy();
                result.addAll(merged.merge(groundedBindings)); // merge itself returns a list
            }
        } else if (resolvedTarget instanceof GroundedAtom) {
            GroundedAtom groundedTarget = (GroundedAtom) resolvedTarget;
            List<Bindings> groundedMatchResults = groundedTarget.match(resolvedPattern);

            for (Bindings groundedBindings : groundedMatchResults) {
                Bindings merged = currentBindings.copy();
                result.addAll(merged.merge(groundedBindings));
            }
        } else {
            if (resolvedPattern.equals(resolvedTarget)) {
                result.add(currentBindings.copy());
            }
        }
        
        // Filter out bindings with loops and ensure distinct results
        return result.stream().filter(b -> !b.hasLoop()).distinct().collect(Collectors.toList());
    }

    public static Atom applyBindings(Atom atom, Bindings bindings) {
        Objects.requireNonNull(atom, "atom cannot be null");
        Objects.requireNonNull(bindings, "bindings cannot be null");

        if (atom instanceof VariableAtom) {
            Atom resolved = bindings.resolve((VariableAtom) atom);
            return resolved == null ? atom : resolved;
        } else if (atom instanceof ExpressionAtom) {
            ExpressionAtom exprAtom = (ExpressionAtom) atom;
            List<Atom> children = exprAtom.getChildren();
            List<Atom> newChildren = null; 

            for (int i = 0; i < children.size(); i++) {
                Atom child = children.get(i);
                Atom newChild = applyBindings(child, bindings);
                if (newChildren == null && newChild != child) { // First change detected
                    newChildren = new ArrayList<>(children.subList(0, i));
                }
                if (newChildren != null) {
                    newChildren.add(newChild);
                }
            }
            return newChildren != null ? new ExpressionAtom(newChildren) : atom;
        } else if (atom instanceof GroundedAtom) {
            GroundedAtom gAtom = (GroundedAtom) atom;
            Object wrappedValue = gAtom.getValue();
            if (wrappedValue instanceof Atom) { 
                Atom newWrappedValue = applyBindings((Atom) wrappedValue, bindings);
                if (newWrappedValue != wrappedValue) {
                    if (gAtom.getValue() == gAtom.getGroundableInterface()) { 
                         return new GroundedAtom(newWrappedValue); 
                    } else { 
                         return new GroundedAtom(newWrappedValue, gAtom.getGroundableInterface());
                    }
                }
            }
            return atom; 
        }
        return atom;
    }
}
