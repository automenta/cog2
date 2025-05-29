package com.example.metta.testing;

import com.example.metta.atom.Atom;
import com.example.metta.interpreter.Interpreter;
import com.example.metta.space.GroundingSpace;
import com.example.metta.space.SpaceReader;
import com.example.metta.text.SExprParser;

import java.util.List;
import java.util.stream.Collectors;

public class MettaTestUtils {

    private static final SExprParser parser = new SExprParser();

    public static Atom atom(String sExpr) {
        try {
            return parser.parse(sExpr);
        } catch (SExprParser.SExprParserException e) {
            throw new RuntimeException("Test utility parsing error for: " + sExpr, e);
        }
    }

    public static List<Atom> interpret(SpaceReader space, String sExpr) {
        Atom parsedExpr = atom(sExpr);
        return Interpreter.interpret(space, parsedExpr);
    }
    
    public static List<String> interpretToStr(SpaceReader space, String sExpr){
        return interpret(space, sExpr).stream()
                .map(Object::toString) // Using Atom.toString which should be suitable for comparison
                .sorted() // Sort to make tests order-independent
                .collect(Collectors.toList());
    }


    public static GroundingSpace createSpace(String sExprSpace) {
        GroundingSpace space = new GroundingSpace();
        if (sExprSpace == null || sExprSpace.trim().isEmpty()) {
            return space;
        }
        List<Atom> atomsInSpace;
        try {
            atomsInSpace = parser.parseToList(sExprSpace);
        } catch (SExprParser.SExprParserException e) {
            throw new RuntimeException("Test utility space creation parsing error for: " + sExprSpace, e);
        }
        for (Atom atom : atomsInSpace) {
            space.add(atom);
        }
        return space;
    }
}
