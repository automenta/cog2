package com.example.metta.atom;

import com.example.metta.interpreter.InterpreterContext;
import com.example.metta.types.Type; // Assuming Type.UNDEFINED is acceptable for now

import java.util.Collections;
import java.util.List;

public abstract class AbstractBinaryOperatorAtom extends GroundedAtom implements Executable {

    public AbstractBinaryOperatorAtom(String symbol) {
        super(symbol);
    }

    // Abstract method to be implemented by concrete operator classes
    protected abstract Number performOperation(Number a, Number b);

    // Optional: Integer-specific operation for precision, if needed.
    // If subclasses can rely on Number and casting, this might not be strictly necessary.
    // For now, let's assume performOperation(Number, Number) is sufficient and handles mixed types.

    @Override
    public List<Atom> execute(InterpreterContext context, List<Atom> args) {
        if (args.size() != 2) {
            // System.err.println("Error: " + getClass().getSimpleName() + " expects 2 arguments, got " + args.size());
            return Collections.singletonList(
                new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("IncorrectArguments"), this))
            );
        }

        Atom arg1 = args.get(0);
        Atom arg2 = args.get(1);

        if (!(arg1 instanceof GroundedAtom) || !(((GroundedAtom) arg1).getGroundedObject() instanceof Number) ||
            !(arg2 instanceof GroundedAtom) || !(((GroundedAtom) arg2).getGroundedObject() instanceof Number)) {
            // System.err.println("Error: " + getClass().getSimpleName() + " expects numeric arguments. Got: " + arg1 + ", " + arg2);
            return Collections.singletonList(
                new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("NonNumericArguments"), this, arg1, arg2))
            );
        }

        Number val1 = (Number) ((GroundedAtom) arg1).getGroundedObject();
        Number val2 = (Number) ((GroundedAtom) arg2).getGroundedObject();

        try {
            Number result = performOperation(val1, val2);
            return Collections.singletonList(new GroundedAtom(result, Type.UNDEFINED)); // Assuming Type.UNDEFINED for now
        } catch (ArithmeticException e) {
            // System.err.println("Error: ArithmeticException in " + getClass().getSimpleName() + ": " + e.getMessage());
             return Collections.singletonList(
                new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("ArithmeticError"), new SymbolAtom(e.getMessage()), this, arg1, arg2))
            );
        }
    }

    // equals method should be implemented by concrete subclasses if they don't add fields.
    // If they are stateless beyond the symbol, then:
    // @Override
    // public boolean equals(Object obj) {
    //     if (this == obj) return true;
    //     if (obj == null || getClass() != obj.getClass()) return false;
    //     AbstractBinaryOperatorAtom other = (AbstractBinaryOperatorAtom) obj;
    //     return this.getSymbol().equals(other.getSymbol());
    // }
    // However, GroundedAtom's equals already checks based on grounded object (symbol for us) and type.
    // Let's rely on GroundedAtom's equals and specific implementations if needed.
}
