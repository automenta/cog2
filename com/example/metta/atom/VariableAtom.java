package com.example.metta.atom;

import java.util.Objects;

/**
 * Represents a variable atom, used in expressions to denote placeholders
 * that can be bound to other atoms during matching or execution.
 * Variable names are stored without the leading '$', which is typically
 * used in their textual representation. Variables are immutable.
 */
public final class VariableAtom extends AbstractAtom {
    private final String name;

    public VariableAtom(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be empty.");
        }
    }

    /**
     * Gets the name of this variable (without the leading '$').
     * @return The non-null, non-empty name of the variable.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VariableAtom)) return false;
        VariableAtom that = (VariableAtom) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        // name.hashCode() is sufficient and correct due to the
        // instanceof check in the equals() method.
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "$" + name;
    }
}
