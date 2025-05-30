package org.opencog.hyperon.metta.interpreter;

import org.opencog.hyperon.atoms.Groundable;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for named {@link Groundable} operations.
 * This allows the {@link MettaEngine} to look up and execute grounded operations
 * that are referenced by their symbolic names in MeTTa expressions.
 */
public class GroundedAtomRegistry {
    private final Map<String, Groundable> operations = new HashMap<>();

    /**
     * Registers a {@link Groundable} operation with a given name.
     * If the name is already registered, the previous operation will be replaced.
     *
     * @param name The name to associate with the operation.
     * @param operation The {@link Groundable} operation to register.
     * @throws IllegalArgumentException if name or operation is null.
     */
    public void registerOperation(String name, Groundable operation) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation name cannot be null or empty.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Groundable operation cannot be null.");
        }
        operations.put(name, operation);
    }

    /**
     * Retrieves a registered {@link Groundable} operation by its name.
     *
     * @param name The name of the operation to retrieve.
     * @return The {@link Groundable} operation, or {@code null} if no operation
     *         is registered with that name.
     */
    public Groundable getOperation(String name) {
        return operations.get(name);
    }

    /**
     * Checks if an operation is registered with the given name.
     *
     * @param name The name to check.
     * @return {@code true} if an operation is registered with the name,
     *         {@code false} otherwise.
     */
    public boolean isRegistered(String name) {
        return operations.containsKey(name);
    }
}
