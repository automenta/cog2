package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.List;

/**
 * Defines the interface for Java objects that can be embedded within a {@link GroundedAtom}.
 * Groundable objects allow bridging Metta with external systems or providing concrete
 * implementations for symbols, enabling custom behavior for execution and matching.
 */
public interface Groundable {
    /**
     * Returns the Metta type of this groundable object.
     * @return An {@link Atom} representing the type.
     */
    Atom getType();

    /**
     * Executes this groundable object with the given arguments.
     * This method defines the operational semantics of the grounded atom.
     * TODO: Consider if the execution context (e.g., current variable {@link Bindings})
     *       should be passed as a parameter, which might be necessary if the
     *       execution can affect or depend on variable bindings.
     * @param args A list of {@link Atom}s serving as arguments to the execution.
     * @return A list of {@link Atom}s representing the result of the execution.
     *         Typically, this will be a single atom or an empty list if there's no result.
     */
    List<Atom> execute(List<Atom> args);

    /**
     * Attempts to match this groundable object against another {@link Atom}.
     * This is crucial for pattern matching in the Metta space.
     * TODO: Consider if the matching context (e.g., current variable {@link Bindings})
     *       should be passed as a parameter or if the method should handle binding
     *       creation and return more complex results (e.g., a stream of binding sets).
     *       The current return of {@code List<Bindings>} implies it can produce multiple
     *       successful binding sets from a single match attempt.
     * @param other The {@link Atom} to match against.
     * @return A list of {@link Bindings} objects. Each Bindings object represents a
     *         successful set of variable assignments if the match succeeds. An empty
     *         list indicates no match or that the match doesn't produce bindings.
     */
    List<Bindings> match(Atom other);

    /**
     * Indicates whether the {@link AtomPrinter} should prefer printing the
     * {@link #toDisplayString()} output directly as a literal, without further quoting.
     * If true, the string is printed as-is. If false (default), the string will be
     * processed by the printer's quoting logic if it contains spaces or special characters.
     * @return {@code true} if the display string should be treated as a literal,
     *         {@code false} otherwise.
     */
    default boolean preferLiteralDisplay() { return false; }

    /**
     * Returns a human-readable string representation of this groundable object.
     * This string is used by the {@link AtomPrinter} when serializing the atom
     * if {@link #preferLiteralDisplay()} is true, or as input to the quoting logic
     * if it's false.
     * @return The display string.
     */
    String toDisplayString();
}
