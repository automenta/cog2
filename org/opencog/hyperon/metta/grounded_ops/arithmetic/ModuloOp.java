package org.opencog.hyperon.metta.grounded_ops.arithmetic;

import org.opencog.hyperon.atoms.Atom;
import org.opencog.hyperon.atoms.Groundable;
import org.opencog.hyperon.atoms.SymbolAtom;

import java.util.List;

public class ModuloOp implements Groundable {
    @Override
    public Atom execute(List<Atom> args) {
        if (args == null || args.size() != 2) {
            throw new IllegalArgumentException("ModuloOp requires exactly two arguments.");
        }
        try {
            // Using double for parsing to allow for inputs like "5.0"
            // but modulo is typically an integer operation.
            // Let's assume integer inputs for modulo, as is common.
            long val1 = Long.parseLong(((SymbolAtom) args.get(0)).getName());
            long val2 = Long.parseLong(((SymbolAtom) args.get(1)).getName());
            if (val2 == 0) {
                throw new ArithmeticException("Modulo by zero.");
            }
            long result = val1 % val2;
            return new SymbolAtom(String.valueOf(result));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arguments to ModuloOp must be integer numbers parsable from SymbolAtom names.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Arguments to ModuloOp must be SymbolAtoms representing integer numbers.", e);
        }
    }
}
