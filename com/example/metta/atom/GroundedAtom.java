package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.List;
import java.util.Objects;

public final class GroundedAtom extends AbstractAtom {
    private final Object value;
    private final Groundable groundableInterface;

    public GroundedAtom(Object value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
        if (value instanceof Groundable) {
            this.groundableInterface = (Groundable) value;
        } else {
            this.groundableInterface = new DefaultGroundable(value);
        }
    }

    public GroundedAtom(Object value, Groundable groundableInterface) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.groundableInterface = Objects.requireNonNull(groundableInterface, "groundableInterface cannot be null");
    }

    public Object getValue() {
        return value;
    }
    
    public Groundable getGroundableInterface() {
        return groundableInterface;
    }

    public Atom getType() {
        return groundableInterface.getType();
    }

    public List<Atom> execute(List<Atom> args) {
        return groundableInterface.execute(args);
    }

    public List<Bindings> match(Atom other) {
        return groundableInterface.match(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GroundedAtom)) return false;
        GroundedAtom that = (GroundedAtom) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return groundableInterface.toDisplayString();
    }
}
