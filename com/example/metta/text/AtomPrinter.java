package com.example.metta.text;

import com.example.metta.atom.*;
import java.util.stream.Collectors;

public class AtomPrinter {

    private AtomPrinter() {
        // Utility class
    }

    private static String quoteStringIfNeeded(String text) {
        // Conditions for quoting are:
        // 1. Empty string.
        // 2. Contains whitespace or characters: ( ) " ' $ # ; (double quote)
        // 3. Can be parsed as a number (float or integer).
        boolean needsQuoting = text.isEmpty() ||
                               text.chars().anyMatch(c -> Character.isWhitespace(c) || "()\"'$#;".indexOf(c) >= 0) ||
                               text.matches("^[+-]?(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?$") || // float
                               text.matches("^[+-]?\\d+(?:[eE][+-]?\\d+)?$");    // integer

        if (needsQuoting) {
            // Escape backslashes first, then quotes, then wrap in quotes
            String escapedText = text.replace("\\", "\\\\") // Replace literal \ with \\
                                     .replace("\"", "\\\"");   // Replace literal " with \"
            return "\"" + escapedText + "\"";
        }
        return text;
    }

    public static String print(Atom atom) {
        if (atom == null) {
            return MettaSymbols.UNDEF_TYPE.toString(); 
        }

        if (atom instanceof SymbolAtom) {
            String name = ((SymbolAtom) atom).getName();
            return quoteStringIfNeeded(name);
        } else if (atom instanceof VariableAtom) {
            return "$" + ((VariableAtom) atom).getName();
        } else if (atom instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) atom;
            return expr.getChildren().stream()
                    .map(AtomPrinter::print) 
                    .collect(Collectors.joining(" ", "(", ")"));
        } else if (atom instanceof GroundedAtom) {
            GroundedAtom groundedAtom = (GroundedAtom) atom;
            Groundable groundable = groundedAtom.getGroundableInterface();
            String displayString = groundable.toDisplayString();
            if (groundable.preferLiteralDisplay()) {
                return displayString;
            } else {
                return quoteStringIfNeeded(displayString);
            }
        } else {
            // Fallback for any unknown Atom type
            System.err.println("Warning: AtomPrinter.print encountered an unexpected Atom type: " + atom.getClass().getName());
            return atom.toString(); 
        }
    }
}
