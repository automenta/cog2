package com.example.metta.text;

import com.example.metta.atom.*;
import java.util.stream.Collectors;

public class AtomPrinter {

    private AtomPrinter() {
        // Utility class
    }

    public static String print(Atom atom) {
        if (atom == null) {
            return MettaSymbols.UNDEF_TYPE.toString(); 
        }

        if (atom instanceof SymbolAtom) {
            String name = ((SymbolAtom) atom).getName();
            // More robust quoting: if a symbol contains whitespace, parens, quotes, $, ;, #
            // or if it's empty, or if it could be parsed as a number.
            // This ensures that what is printed can be parsed back as a single symbol by SExprParser.
            if (name.isEmpty() || 
                name.chars().anyMatch(c -> Character.isWhitespace(c) || "()\"'$#;".indexOf(c) >= 0) ||
                name.matches("^[+-]?(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?$") || // float
                name.matches("^[+-]?\\d+(?:[eE][+-]?\\d+)?$") ) { // integer (potentially with exponent)
                return "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
            return name;
        } else if (atom instanceof VariableAtom) {
            return "$" + ((VariableAtom) atom).getName();
        } else if (atom instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) atom;
            return expr.getChildren().stream()
                    .map(AtomPrinter::print) 
                    .collect(Collectors.joining(" ", "(", ")"));
        } else if (atom instanceof GroundedAtom) {
            // GroundedAtom's Groundable interface is responsible for its string representation.
            return ((GroundedAtom) atom).getGroundableInterface().toDisplayString();
        } else {
            // Fallback for any unknown Atom type
            System.err.println("Warning: AtomPrinter.print encountered an unexpected Atom type: " + atom.getClass().getName());
            return atom.toString(); 
        }
    }
}
