package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList; // Added missing import

public class DefaultGroundable implements Groundable {
    private final Object value;

    public DefaultGroundable(Object value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    @Override
    public Atom getType() {
        return new SymbolAtom("Java." + value.getClass().getSimpleName());
    }

    @Override
    public List<Atom> execute(List<Atom> args) {
        throw new UnsupportedOperationException("DefaultGroundable is not executable: " + value.toString());
    }

    @Override
    public List<Bindings> match(Atom other) {
        if (other instanceof GroundedAtom) {
            Object otherValue = ((GroundedAtom) other).getValue();
            if (Objects.equals(this.value, otherValue)) {
                List<Bindings> result = new ArrayList<>(1); 
                result.add(new Bindings()); 
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String toDisplayString() {
        return value.toString();
    }

    @Override
    public boolean preferLiteralDisplay() {
        return this.value instanceof Number;
    }

    public Object getValue() {
        return value;
    }
}
