package com.example.metta.atom;

import com.example.metta.types.Type;
import com.example.metta.interpreter.InterpreterContext;
import java.util.List;
import java.util.Collections;

public class MultiplyAtom extends AbstractBinaryOperatorAtom {

    public MultiplyAtom() {
        super("*");
    }

    @Override
    protected Number performOperation(Number a, Number b) {
        // Promote to double if either is double, otherwise perform integer multiplication.
        if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() * b.doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() * b.floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() * b.longValue();
        } else {
            // Default to integer multiplication for Integer, Short, Byte
            return a.intValue() * b.intValue();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MultiplyAtom)) return false;
        return super.equals(obj);
    }
}
