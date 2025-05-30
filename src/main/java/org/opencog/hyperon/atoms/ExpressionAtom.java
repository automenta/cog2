package org.opencog.hyperon.atoms;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;


public class ExpressionAtom implements Atom {
    private final List<Atom> children;

    public ExpressionAtom(List<Atom> children) {
        // Store an immutable copy
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override
    public AtomType getType() {
        return AtomType.EXPRESSION;
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Expressions do not have a name.");
    }

    @Override
    public List<Atom> getChildren() {
        return this.children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpressionAtom that = (ExpressionAtom) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    @Override
    public String toString() {
        return "(" + children.stream().map(Atom::toString).collect(Collectors.joining(" ")) + ")";
    }
}
