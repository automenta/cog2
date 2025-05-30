package org.opencog.hyperon.metta.grounded_ops.arithmetic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class MultiplyOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            // Or return a special ErrorAtom or throw a more specific MeTTa related exception
            throw new IllegalArgumentException("MultiplyOp requires exactly two arguments.");
        }
        try {
            double val1 = Double.parseDouble(((SymbolAtom) args.get(0)).getName());
            double val2 = Double.parseDouble(((SymbolAtom) args.get(1)).getName());
            double product = val1 * val2;
            // Represent result as a SymbolAtom. Handle integers cleanly.
            if (product == (long) product) {
                return new SymbolAtom(String.valueOf((long) product));
            } else {
                return new SymbolAtom(String.valueOf(product));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments to MultiplyOp must be numbers parsable from SymbolAtom names.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to MultiplyOp must be SymbolAtoms representing numbers.", e);
        }
    }
}
