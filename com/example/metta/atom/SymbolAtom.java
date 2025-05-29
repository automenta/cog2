package com.example.metta.atom;

import java.util.Objects;

public final class SymbolAtom extends AbstractAtom {
    private final String name;

    public SymbolAtom(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

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
