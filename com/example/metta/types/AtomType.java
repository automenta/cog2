package com.example.metta.types;

import com.example.metta.atom.Atom;
import com.example.metta.atom.ExpressionAtom;
import com.example.metta.atom.MettaSymbols;

import java.util.Objects;

public class AtomType {
    private final Atom typeAtom;
    private final boolean isFunction;
    private final boolean isApplicationResult;
    private final boolean isError;

    public AtomType(Atom typeAtom, boolean isApplicationResult, boolean isError) {
        this.typeAtom = Objects.requireNonNull(typeAtom, "typeAtom cannot be null");
        this.isApplicationResult = isApplicationResult;
        this.isError = isError;
        this.isFunction = (typeAtom instanceof ExpressionAtom) &&
                          !((ExpressionAtom) typeAtom).getChildren().isEmpty() &&
                          ((ExpressionAtom) typeAtom).getChildren().get(0).equals(MettaSymbols.ARROW_SYMBOL);
    }

    public Atom getAtom() {
        return typeAtom;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public boolean isApplicationResult() {
        return isApplicationResult;
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public String toString() {
        // Using a more compact representation, flags only if true.
        StringBuilder sb = new StringBuilder("Type(");
        sb.append(typeAtom);
        if (isFunction) sb.append(", Fn");
        if (isApplicationResult) sb.append(", AppR");
        if (isError) sb.append(", Err");
        sb.append(')');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomType)) return false; // Simplified type check
        AtomType atomType = (AtomType) o;
        return isFunction == atomType.isFunction &&
                isApplicationResult == atomType.isApplicationResult &&
                isError == atomType.isError &&
                typeAtom.equals(atomType.typeAtom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeAtom, isFunction, isApplicationResult, isError);
    }
}
