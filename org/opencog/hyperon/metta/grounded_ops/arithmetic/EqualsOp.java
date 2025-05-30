package org.opencog.hyperon.metta.grounded_ops.arithmetic; // Package might be more general later, e.g., common_ops

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class EqualsOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            throw new IllegalArgumentException("EqualsOp requires exactly two arguments.");
        }
        // Perform comparison based on Atom.equals()
        boolean areEqual = args.get(0).equals(args.get(1));

        // Return SymbolAtom "True" or "False"
        // These are conventional True/False symbols in MeTTa / OpenCog Classic
        if (areEqual) {
            return new SymbolAtom("True");
        } else {
            return new SymbolAtom("False");
        }
    }
}
