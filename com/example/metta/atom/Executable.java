package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.List;

/**
 * An interface for {@link Groundable} atoms that can be executed by the interpreter
 * using specific arguments and access to current variable bindings.
 *
 * This interface provides a more specific version of the execute method
 * from {@link Groundable} for executables that require binding information.
 */
public interface Executable extends Groundable {

    /**
     * Executes this groundable object with the given arguments and variable bindings.
     * This method defines the operational semantics of the executable grounded atom.
     *
     * @param args A list of {@link Atom}s serving as arguments to the execution.
     * @param bindings The current {@link Bindings} in the execution context.
     * @return A list of {@link Atom}s representing the result of the execution.
     *         Typically, this will be a single atom or an empty list if there's no result.
     */
    List<Atom> execute(List<Atom> args, Bindings bindings);

    /**
     * Default implementation for the execute method inherited from Groundable.
     * Groundable objects that are Executable should primarily use the
     * execute(args, bindings) method. This default implementation can
     * be overridden if there's a specific behavior desired for the no-bindings
     * execute call, otherwise it could throw an UnsupportedOperationException
     * or delegate to the bindings-aware version with empty/default bindings.
     * For now, let's make it call the more specific execute with null bindings,
     * though implementations should be aware of this.
     */
    @Override
    default List<Atom> execute(List<Atom> args) {
        // This default implementation calls the more specific execute method.
        // Implementations of Executable should ideally provide their logic in
        // execute(List<Atom> args, Bindings bindings).
        // Passing null for bindings here might not be ideal for all cases;
        // specific executables might need to override this default method from Groundable
        // if they can operate without bindings or need a different default.
        return execute(args, null); // Or consider: throw new UnsupportedOperationException("This executable requires bindings.");
    }
}
