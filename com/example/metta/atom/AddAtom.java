package com.example.metta.atom;

import com.example.metta.types.Type;
// InterpreterContext, List, Collections might not be strictly needed here anymore
// but leaving them for now.
import com.example.metta.interpreter.InterpreterContext;
import java.util.List;
import java.util.Collections;

public class AddAtom extends AbstractBinaryOperatorAtom {

    public AddAtom() {
        super("+");
    }

    @Override
    protected Number performOperation(Number a, Number b) {
        // Promote to double if either is double, otherwise perform integer addition.
        if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() + b.doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            // Handle floats explicitly if necessary, or let them be promoted to double
            return a.floatValue() + b.floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue();
        } else {
            // Default to integer addition for Integer, Short, Byte
            return a.intValue() + b.intValue();
        }
    }

    @Override
    public boolean equals(Object obj) {
        // Rely on GroundedAtom's equals, which checks the symbol for stateless atoms.
        // This implementation assumes AddAtom has no additional state beyond its symbol.
        if (this == obj) return true;
        if (!(obj instanceof AddAtom)) return false;
        // If GroundedAtom's equals considers the class type, this is fine.
        // If it only considers the grounded object (symbol), this is also fine.
        return super.equals(obj);
    }
}
