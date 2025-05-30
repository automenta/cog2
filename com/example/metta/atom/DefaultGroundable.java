package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList; // Added missing import

/**
 * A default implementation of the {@link Groundable} interface.
 * This class wraps an arbitrary Java object and provides baseline behaviors:
 * <ul>
 *   <li>Type is derived from the object's Java class name (e.g., "Java.Integer").</li>
 *   <li>Execution is unsupported by default.</li>
 *   <li>Matching is based on simple equality of the wrapped Java objects.</li>
 *   <li>Display string is the result of the object's {@code toString()} method.</li>
 *   <li>{@code preferLiteralDisplay} returns true for {@link Number} instances.</li>
 * </ul>
 */
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

    /**
     * Gets the underlying Java object wrapped by this DefaultGroundable.
     * @return The non-null Java object.
     */
    public Object getValue() {
        return value;
    }
}
