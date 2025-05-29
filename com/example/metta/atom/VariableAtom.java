package com.example.metta.atom;

import java.util.Objects;

public final class VariableAtom extends AbstractAtom {
    private final String name;

    public VariableAtom(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

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
        // The name itself plus a constant to distinguish from SymbolAtom if names were same.
        // Since VariableAtom has "$" prepended in toString, its role is distinct.
        // Simple name.hashCode() is fine as equals() checks type.
        // Adding a prime number helps reduce collisions if used in same hash table as SymbolAtom
        // and names could be identical (which they can't be if one is "$foo" and other "foo" from parser).
        // However, direct name.hashCode() is simplest and correct given the type check in equals.
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "$" + name;
    }
}
