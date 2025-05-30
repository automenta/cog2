package org.opencog.hyperon.atoms;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GroundedAtom implements Atom {
    private final String name; // For display and identification
    private final Groundable operation;

    public GroundedAtom(String name, Groundable operation) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("GroundedAtom name cannot be null or empty.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("GroundedAtom operation cannot be null.");
        }
        this.name = name;
        this.operation = operation;
    }

    @Override
    public AtomType getType() {
        return AtomType.GROUNDED;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Atom> getChildren() {
        // GroundedAtoms, by this definition, do not have children in the sense
        // ExpressionAtoms do. Their operation might take arguments, but those are not
        // considered children of the atom itself.
        return Collections.emptyList();
    }

    /**
     * Executes the grounded operation with the provided arguments.
     *
     * @param args The arguments for the operation.
     * @return The result of the operation.
     */
    public Atom executeOperation(List<Atom> args) {
        return this.operation.execute(args);
    }

    public Groundable getOperation() {
        return this.operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroundedAtom that = (GroundedAtom) o;
        // Equality based on name and the class of the operation.
        // This assumes names are unique for a given operation type,
        // or that comparing operation instances directly is too complex/unreliable.
        return name.equals(that.name) &&
               operation.getClass().equals(that.operation.getClass());
    }

    @Override
    public int hashCode() {
        // Hash code based on name and the class of the operation.
        return Objects.hash(name, operation.getClass());
    }

    @Override
    public String toString() {
        // A common representation for grounded atoms might be just their name,
        // or include some indication of their grounded nature, e.g., #name
        // For now, let's stick to the name for simplicity, as per getName().
        return name;
    }
}
