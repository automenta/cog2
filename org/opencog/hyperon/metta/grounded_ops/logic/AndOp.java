package org.opencog.hyperon.metta.grounded_ops.logic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class AndOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            throw new IllegalArgumentException("AndOp requires exactly two arguments.");
        }
        try {
            // Expects SymbolAtoms "True" or "False"
            boolean val1 = "True".equals(((SymbolAtom) args.get(0)).getName());
            boolean val2 = "True".equals(((SymbolAtom) args.get(1)).getName());

            if (val1 && val2) {
                return new SymbolAtom("True");
            } else {
                return new SymbolAtom("False");
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to AndOp must be SymbolAtoms representing boolean 'True' or 'False'.", e);
        }
    }
}
