package org.opencog.hyperon.atoms;

import java.util.List;

public interface Atom {
    AtomType getType();
    String getName() throws UnsupportedOperationException;
    List<Atom> getChildren() throws UnsupportedOperationException;
    boolean equals(Object o);
    int hashCode();

    enum AtomType {
        SYMBOL,
        VARIABLE,
        EXPRESSION,
        GROUNDED
    }
}
