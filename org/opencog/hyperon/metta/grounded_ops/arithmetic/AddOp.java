package org.opencog.hyperon.metta.grounded_ops.arithmetic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class AddOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            // Or return a special ErrorAtom or throw a more specific MeTTa related exception
            throw new IllegalArgumentException("AddOp requires exactly two arguments.");
        }
        try {
            // Assuming numbers are represented as SymbolAtoms containing parsable integer strings
            double val1 = Double.parseDouble(((SymbolAtom) args.get(0)).getName());
            double val2 = Double.parseDouble(((SymbolAtom) args.get(1)).getName());
            double sum = val1 + val2;
            // Represent result as a SymbolAtom. Consider if a NumberAtom type would be better.
            // For now, use String representation. Handle integers cleanly.
            if (sum == (long) sum) {
                return new SymbolAtom(String.valueOf((long) sum));
            } else {
                return new SymbolAtom(String.valueOf(sum));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments to AddOp must be numbers parsable from SymbolAtom names.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to AddOp must be SymbolAtoms representing numbers.", e);
        }
    }
}
