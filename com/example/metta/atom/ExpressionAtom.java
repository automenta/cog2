package com.example.metta.atom;

import java.util.List;
import java.util.Objects;
// Unused: import java.util.ArrayList;
// Unused: import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Represents an expression atom, which is an ordered sequence of other atoms.
 * Expressions are used to form complex structures, function calls, or nested statements
 * in Metta. They are immutable. For example, {@code (f a b)} represents an
 * expression where {@code f}, {@code a}, and {@code b} are child atoms.
 */
public final class ExpressionAtom extends AbstractAtom {
    private final List<Atom> children;

    public ExpressionAtom(List<Atom> children) {
        Objects.requireNonNull(children, "children cannot be null");
        this.children = List.copyOf(children); // Java 10+ style defensive copy
    }

    /**
     * Gets the children of this expression.
     * The returned list is unmodifiable.
     * @return An unmodifiable {@link List} of {@link Atom}s representing the children.
     */
    public List<Atom> getChildren() {
        return children; // Already unmodifiable from List.copyOf()
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ExpressionAtom)) return false;
        ExpressionAtom that = (ExpressionAtom) obj;
        return children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return children.hashCode();
    }

    @Override
    public String toString() {
        return children.stream()
                .map(String::valueOf) // Handles nulls gracefully if they could appear, though list disallows nulls
                .collect(Collectors.joining(" ", "(", ")"));
    }
}
