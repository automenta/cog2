package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.List;
import java.util.Objects;

/**
 * Represents a grounded atom, which embeds a native Java object (the "value")
 * into the Metta system. The behavior of this atom (how it executes, matches,
 * and is typed) is defined by an associated {@link Groundable} interface.
 * GroundedAtoms are immutable in their structure, though the underlying Java
 * value or Groundable instance might be mutable.
 */
public final class GroundedAtom extends AbstractAtom {
    private final Object value;
    private final Groundable groundableInterface;

    /**
     * Constructs a GroundedAtom with a given Java object.
     * If the provided value is itself an instance of {@link Groundable}, it's used
     * directly as the behavior interface. Otherwise, the value is wrapped in a
     * {@link DefaultGroundable} instance.
     * @param value The Java object to be grounded. Must not be null.
     */
    public GroundedAtom(Object value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
        if (value instanceof Groundable) {
            this.groundableInterface = (Groundable) value;
        } else {
            this.groundableInterface = new DefaultGroundable(value);
        }
    }

    /**
     * Constructs a GroundedAtom with a given Java object and an explicit
     * {@link Groundable} interface implementation.
     * @param value The Java object to be grounded. Must not be null.
     * @param groundableInterface The specific Groundable implementation defining
     *                            this atom's behavior. Must not be null.
     */
    public GroundedAtom(Object value, Groundable groundableInterface) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.groundableInterface = Objects.requireNonNull(groundableInterface, "groundableInterface cannot be null");
    }

    /**
     * Gets the underlying Java object this atom is grounding.
     * @return The non-null Java object.
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Gets the {@link Groundable} interface that defines the behavior
     * (type, execution, matching, display) of this grounded atom.
     * @return The non-null Groundable interface implementation.
     */
    public Groundable getGroundableInterface() {
        return groundableInterface;
    }

    /**
     * {@inheritDoc}
     * <p>Delegates to the associated {@link Groundable#getType()}.
     */
    public Atom getType() {
        return groundableInterface.getType();
    }

    /**
     * {@inheritDoc}
     * <p>Delegates to the associated {@link Groundable#execute(List)}.
     */
    public List<Atom> execute(List<Atom> args) {
        return groundableInterface.execute(args);
    }

    /**
     * {@inheritDoc}
     * <p>Delegates to the associated {@link Groundable#match(Atom)}.
     */
    public List<Bindings> match(Atom other) {
        return groundableInterface.match(other);
    }

    /**
     * Compares this GroundedAtom to another object for equality.
     * Two GroundedAtoms are considered equal if their underlying Java {@link #getValue()}
     * objects are equal according to their own {@code equals} method.
     * The specific {@link Groundable} interface implementation is not considered
     * in this equality check.
     * @param obj The object to compare with.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GroundedAtom)) return false;
        GroundedAtom that = (GroundedAtom) obj;
        return value.equals(that.value);
    }

    /**
     * Returns a hash code value for the atom.
     * The hash code is based on the hash code of the underlying Java {@link #getValue()} object.
     * @return A hash code value for this atom.
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Returns the string representation of this atom, as defined by its
     * {@link Groundable#toDisplayString()} method. This string will be used by
     * {@link com.example.metta.text.AtomPrinter}, which may further process it
     * based on {@link Groundable#preferLiteralDisplay()}.
     * @return The string representation from the Groundable interface.
     */
    @Override
    public String toString() {
        return groundableInterface.toDisplayString();
    }
}
