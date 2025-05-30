package org.opencog.hyperon.metta.grounded_ops.arithmetic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class LessThanOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            throw new IllegalArgumentException("LessThanOp requires exactly two arguments.");
        }
        try {
            // Using double for comparison flexibility
            double val1 = Double.parseDouble(((SymbolAtom) args.get(0)).getName());
            double val2 = Double.parseDouble(((SymbolAtom) args.get(1)).getName());

            if (val1 < val2) {
                return new SymbolAtom("True");
            } else {
                return new SymbolAtom("False");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments to LessThanOp must be numbers parsable from SymbolAtom names.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to LessThanOp must be SymbolAtoms representing numbers.", e);
        }
    }
}
