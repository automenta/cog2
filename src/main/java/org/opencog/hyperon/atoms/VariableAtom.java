package org.opencog.hyperon.atoms;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VariableAtom implements Atom {
    private final String name;

    public VariableAtom(String name) {
        if (name == null || !name.startsWith("$") || name.length() == 1) {
            throw new IllegalArgumentException("Variable name must start with $ and have a non-empty name");
        }
        this.name = name;
    }

    @Override
    public AtomType getType() {
        return AtomType.VARIABLE;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Atom> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableAtom that = (VariableAtom) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
