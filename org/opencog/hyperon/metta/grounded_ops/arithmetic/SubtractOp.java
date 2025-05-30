package org.opencog.hyperon.metta.grounded_ops.arithmetic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class SubtractOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            throw new IllegalArgumentException("SubtractOp requires exactly two arguments.");
        }
        try {
            double val1 = Double.parseDouble(((SymbolAtom) args.get(0)).getName());
            double val2 = Double.parseDouble(((SymbolAtom) args.get(1)).getName());
            double difference = val1 - val2;
            if (difference == (long) difference) {
                return new SymbolAtom(String.valueOf((long) difference));
            } else {
                return new SymbolAtom(String.valueOf(difference));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments to SubtractOp must be numbers parsable from SymbolAtom names.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to SubtractOp must be SymbolAtoms representing numbers.", e);
        }
    }
}
