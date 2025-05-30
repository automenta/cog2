package com.example.metta.atom;

import java.util.Objects;

/**
 * Represents a symbolic atom, a unique constant identifier.
 * Symbols are fundamental building blocks in Metta expressions, analogous to
 * symbols in Lisp or atoms in Prolog. They are immutable.
 */
public final class SymbolAtom extends AbstractAtom {
    private final String name;

    public SymbolAtom(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    /**
     * Gets the name of this symbol.
     * @return The non-null name of the symbol.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SymbolAtom)) return false;
        SymbolAtom that = (SymbolAtom) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
