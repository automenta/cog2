package org.opencog.hyperon.space;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.ExpressionAtom;
import org.opencog.hyperon.atoms.VariableAtom;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Bindings {
    private final Map<VariableAtom, Atom> bindingsMap;

    public Bindings() {
        this.bindingsMap = Collections.unmodifiableMap(new HashMap<>());
    }

    public Bindings(Map<VariableAtom, Atom> initialBindings) {
        if (initialBindings == null) {
            this.bindingsMap = Collections.unmodifiableMap(new HashMap<>());
        } else {
            // Ensure keys and values are not null, defensively
            for (Map.Entry<VariableAtom, Atom> entry : initialBindings.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("Binding variable or value cannot be null.");
                }
            }
            this.bindingsMap = Collections.unmodifiableMap(new HashMap<>(initialBindings));
        }
    }

    private Bindings(Map<VariableAtom, Atom> internalMap, boolean isInternal) {
        // Private constructor for internal use to avoid copying again
        this.bindingsMap = Collections.unmodifiableMap(internalMap);
    }


    public boolean isBound(VariableAtom var) {
        return bindingsMap.containsKey(var);
    }

    public Atom getValue(VariableAtom var) {
        return bindingsMap.get(var);
    }

    public Bindings addBinding(VariableAtom var, Atom value) {
        if (var == null || value == null) {
            throw new IllegalArgumentException("Binding variable or value cannot be null.");
        }
        if (bindingsMap.containsKey(var)) {
            if (Objects.equals(bindingsMap.get(var), value)) {
                return this; // Already bound to the same value
            } else {
                throw new IllegalStateException("Variable " + var.getName() +
                        " already bound to a different value: " + bindingsMap.get(var) +
                        ", trying to bind to: " + value);
            }
        }
        Map<VariableAtom, Atom> newMap = new HashMap<>(this.bindingsMap);
        newMap.put(var, value);
        return new Bindings(newMap, true);
    }

    public Map<VariableAtom, Atom> getMap() {
        return bindingsMap; // Already unmodifiable
    }

    public Atom substitute(Atom target) {
        if (target instanceof VariableAtom) {
            VariableAtom varTarget = (VariableAtom) target;
            return bindingsMap.getOrDefault(varTarget, varTarget);
        } else if (target instanceof ExpressionAtom) {
            ExpressionAtom exprTarget = (ExpressionAtom) target;
            List<Atom> originalChildren = exprTarget.getChildren();
            List<Atom> substitutedChildren = originalChildren.stream()
                    .map(this::substitute)
                    .collect(Collectors.toList());
            // Optimization: if no children were changed, return the original expression atom
            boolean changed = false;
            if (originalChildren.size() != substitutedChildren.size()) { // Should not happen with current logic
                changed = true;
            } else {
                for (int i = 0; i < originalChildren.size(); i++) {
                    if (originalChildren.get(i) != substitutedChildren.get(i)) {
                        changed = true;
                        break;
                    }
                }
            }
            return changed ? new ExpressionAtom(substitutedChildren) : exprTarget;
        }
        return target; // SymbolAtom, GroundedAtom, or unbound VariableAtom
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bindings bindings = (Bindings) o;
        return Objects.equals(bindingsMap, bindings.bindingsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindingsMap);
    }

    @Override
    public String toString() {
        return "Bindings{" +
                bindingsMap.entrySet().stream()
                        .map(entry -> entry.getKey().getName() + "=" + entry.getValue().toString())
                        .collect(Collectors.joining(", ")) +
                '}';
    }
}
