package org.opencog.hyperon.atoms;

import java.util.List;

/**
 * Represents an operation that can be executed.
 * This is typically used by GroundedAtoms.
 */
public interface Groundable {
    /**
     * Executes the operation with the given arguments.
     *
     * @param args The list of atoms serving as arguments to the operation.
     * @return The resulting atom from the execution.
     *         This might be a simple atom (Symbol, Variable, Grounded) or an ExpressionAtom.
     *         Implementations might throw runtime exceptions if arguments are invalid
     *         or if execution fails.
     */
    Atom execute(List<Atom> args);
}
