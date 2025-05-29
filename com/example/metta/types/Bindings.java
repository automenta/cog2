package com.example.metta.types;

import com.example.metta.atom.Atom;
import com.example.metta.atom.VariableAtom;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;

public class Bindings {

    private final Map<VariableAtom, VariableAtom> parent;
    private final Map<VariableAtom, Atom> valueBindings;

    public Bindings() {
        this.parent = new HashMap<>();
        this.valueBindings = new HashMap<>();
    }

    private Bindings(Map<VariableAtom, VariableAtom> parent, Map<VariableAtom, Atom> valueBindings) {
        this.parent = new HashMap<>(parent);
        this.valueBindings = new HashMap<>(valueBindings);
    }

    // DSU find operation with path compression
    public VariableAtom find(VariableAtom var) {
        parent.putIfAbsent(var, var);
        if (parent.get(var).equals(var)) {
            return var;
        }
        VariableAtom root = find(parent.get(var));
        parent.put(var, root);
        return root;
    }

    // DSU union operation
    private boolean union(VariableAtom var1, VariableAtom var2) {
        VariableAtom root1 = find(var1);
        VariableAtom root2 = find(var2);

        if (root1.equals(root2)) {
            return true;
        }

        Atom value1 = valueBindings.get(root1);
        Atom value2 = valueBindings.get(root2);

        if (value1 != null && value2 != null && !value1.equals(value2)) {
            return false; 
        }

        parent.put(root2, root1);

        if (value1 == null && value2 != null) {
            valueBindings.put(root1, value2);
            valueBindings.remove(root2);
        } else if (value1 != null && value2 == null) {
            valueBindings.remove(root2); 
        }
        return true;
    }

    public Atom resolve(VariableAtom var) {
        return resolveRecursive(var, new HashSet<>());
    }

    private Atom resolveRecursive(VariableAtom var, Set<VariableAtom> visitedInPath) {
        if (!visitedInPath.add(var)) {
            return var; 
        }

        VariableAtom root = find(var);
        Atom value = valueBindings.get(root);

        Atom result;
        if (value instanceof VariableAtom) {
            result = resolveRecursive((VariableAtom) value, visitedInPath);
            if (result != null && !result.equals(value) && !(result instanceof VariableAtom && visitedInPath.contains(result))) {
                 valueBindings.put(root, result);
            }
        } else {
            result = value;
        }
        
        visitedInPath.remove(var); 
        return result;
    }

    public boolean addVariableEquality(VariableAtom var1, VariableAtom var2) {
        Objects.requireNonNull(var1, "var1 cannot be null");
        Objects.requireNonNull(var2, "var2 cannot be null");
        return union(var1, var2);
    }

    public boolean addValueBinding(VariableAtom var, Atom value) {
        Objects.requireNonNull(var, "var cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        if (value instanceof VariableAtom) {
            return addVariableEquality(var, (VariableAtom) value);
        }

        VariableAtom root = find(var);
        Atom existingValue = resolveRecursive(var, new HashSet<>()); // Resolve fully before checking

        if (existingValue != null && !(existingValue instanceof VariableAtom) && !existingValue.equals(value)) {
            return false; 
        }
        
        // If existingValue is a variable, binding root to 'value' is fine and might merge sets if 'value' was also a var.
        // If existingValue is null, or equal to value, also fine.
        valueBindings.put(root, value);
        return true;
    }

    public List<Bindings> merge(Bindings other) {
        Objects.requireNonNull(other, "other Bindings cannot be null");
        Bindings merged = this.copy();

        for (Map.Entry<VariableAtom, VariableAtom> entry : other.parent.entrySet()) {
            VariableAtom otherVar = entry.getKey();
            VariableAtom otherParent = other.find(otherVar); 
            if (!otherVar.equals(otherParent)) {
                if (!merged.addVariableEquality(otherVar, otherParent)) {
                    return Collections.emptyList(); 
                }
            }
        }
        
        for (Map.Entry<VariableAtom, Atom> entry : other.valueBindings.entrySet()) {
            VariableAtom varInOther = entry.getKey(); 
            Atom valueInOther = entry.getValue();

            if (!merged.addValueBinding(varInOther, valueInOther)) {
                 return Collections.emptyList();
            }
        }
        return Collections.singletonList(merged);
    }

    public boolean isCompatibleWith(Bindings other) {
        return !this.merge(other).isEmpty();
    }

    public boolean isEmpty() {
        if (!valueBindings.isEmpty()) return false;
        for (VariableAtom var : parent.keySet()) {
            if (!parent.get(var).equals(var)) { // An actual parent link, not just var -> var from find()
                 // Check if this var is part of a non-trivial equality set by finding its root
                 if (!find(var).equals(var)) return false;
            }
        }
        // After checking all, if all vars are their own roots (after path compression by find),
        // and valueBindings is empty, then the Bindings is empty.
        // A simpler check: if valueBindings is empty and parent map is empty (no variables ever seen).
        // Or, if all variables seen are their own canonical representatives.
        if (valueBindings.isEmpty()) {
            for (VariableAtom k : parent.keySet()) {
                if (!k.equals(find(k))) return false; // k is part of a set represented by another variable
            }
        } else {
            return false; // Has value bindings
        }
        return true; // No value bindings and all known vars are roots of their own singleton sets
    }


    public Bindings copy() {
        return new Bindings(this.parent, this.valueBindings);
    }

    @Override
    public String toString() {
        Set<VariableAtom> allVars = new HashSet<>();
        parent.keySet().forEach(allVars::add);
        valueBindings.keySet().forEach(allVars::add); // Add keys from valueBindings too

        String bindingsStr = allVars.stream()
            .map(var -> {
                // For display, show var -> resolvedValue or var -> its_canonical_representative
                Atom resolvedValue = resolve(var); // Use public resolve
                if (resolvedValue != null && !resolvedValue.equals(var)) {
                    return var.toString() + ":=" + resolvedValue.toString();
                } else if (resolvedValue == null && !find(var).equals(var)) { 
                    // Unbound but part of an equality set. Show its canonical rep.
                     return var.toString() + ":=" + find(var).toString();
                }
                // If var is its own root and has no value binding, don't print it unless it was explicitly a key in valueBindings (which resolve would have handled)
                // Or if it was a key in parent map initially (which find(var) ensures).
                // Essentially, only print if there's a meaningful binding or equality to show.
                // The current logic might print "$v:=$v" if it's a root and unbound; filter that.
                return null; 
            })
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.joining(", "));
        
        return "Bindings{" + (bindingsStr.isEmpty() ? "<empty>" : bindingsStr) + "}";
    }

    public boolean hasLoop() {
        Set<VariableAtom> allKnownVariables = new HashSet<>(parent.keySet());
        valueBindings.keySet().forEach(k -> allKnownVariables.add(find(k))); // Add canonical vars from valueBindings
        
        for (VariableAtom var : allKnownVariables) {
            Set<VariableAtom> visitedInPath = new HashSet<>();
            VariableAtom current = var;
            
            while (current != null) {
                if (!visitedInPath.add(current)) {
                    return true; // Loop detected
                }
                Atom value = valueBindings.get(find(current)); // Resolve via canonical parent
                if (value instanceof VariableAtom) {
                    current = (VariableAtom) value;
                } else {
                    current = null; // Path ends in a non-variable or null
                }
            }
        }
        return false;
    }
}
