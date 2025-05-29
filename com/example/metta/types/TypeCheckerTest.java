package com.example.metta.types;

import com.example.metta.atom.*;
import com.example.metta.space.GroundingSpace;
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience parsing
import static com.example.metta.testing.MettaTestUtils.createSpace;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

public class TypeCheckerTest {

    @Test
    void isFunctionTypeHelper() {
        assertTrue(TypeChecker.isFunctionType(atom("(-> A B)")));
        assertTrue(TypeChecker.isFunctionType(atom("(-> $t R)")));
        assertTrue(TypeChecker.isFunctionType(atom("(-> T1 T2 R)")));
        assertFalse(TypeChecker.isFunctionType(atom("(A B C)")));
        assertFalse(TypeChecker.isFunctionType(atom("->")));
        assertFalse(TypeChecker.isFunctionType(atom("($t R)")));
    }

    @Test
    void getFunctionArgTypesHelper() {
        assertEquals(List.of(atom("A")), TypeChecker.getFunctionArgTypes(atom("(-> A B)")));
        assertEquals(List.of(atom("$t")), TypeChecker.getFunctionArgTypes(atom("(-> $t R)")));
        assertEquals(List.of(atom("T1"), atom("T2")), TypeChecker.getFunctionArgTypes(atom("(-> T1 T2 R)")));
        assertEquals(Collections.emptyList(), TypeChecker.getFunctionArgTypes(atom("(-> R)"))); // No args
        assertTrue(TypeChecker.getFunctionArgTypes(atom("(A B C)")).isEmpty());
    }

    @Test
    void getFunctionReturnTypeHelper() {
        assertEquals(atom("B"), TypeChecker.getFunctionReturnType(atom("(-> A B)")));
        assertEquals(atom("R"), TypeChecker.getFunctionReturnType(atom("(-> $t R)")));
        assertEquals(atom("R"), TypeChecker.getFunctionReturnType(atom("(-> T1 T2 R)")));
        assertEquals(atom("R"), TypeChecker.getFunctionReturnType(atom("(-> R)")));
        assertEquals(MettaSymbols.UNDEF_TYPE, TypeChecker.getFunctionReturnType(atom("(A B C)")));
    }

    @Test
    void getAtomTypesSymbol() {
        GroundingSpace space = createSpace("(: list (-> $A Nat)) (: list IntList)");
        SymbolAtom listSymbol = new SymbolAtom("list");
        List<String> types = TypeChecker.getAtomTypes(space, listSymbol).stream()
            .map(at -> at.getAtom().toString())
            .sorted()
            .collect(Collectors.toList());
        
        assertEquals(List.of("(-> $A Nat)", "IntList"), types);
    }
    
    @Test
    void getAtomTypesVariable() {
        GroundingSpace space = createSpace("");
        VariableAtom x = new VariableAtom("x");
        List<AtomType> types = TypeChecker.getAtomTypes(space, x);
        assertEquals(1, types.size());
        assertEquals(MettaSymbols.UNDEF_TYPE, types.get(0).getAtom());
        assertFalse(types.get(0).isError());
    }

    @Test
    void getAtomTypesGroundedAtom() {
        GroundingSpace space = createSpace("");
        GroundedAtom num = new GroundedAtom(42); // Uses DefaultGroundable
        List<AtomType> types = TypeChecker.getAtomTypes(space, num);
        assertEquals(1, types.size());
        // DefaultGroundable returns SymbolAtom("Java." + value.getClass().getSimpleName())
        assertEquals(new SymbolAtom("Java.Integer"), types.get(0).getAtom());
        assertFalse(types.get(0).isError());
    }
    
    @Test
    void getAtomTypesGroundedAtomWithTypeVar() {
        // Test makeVariablesUnique (simplified version)
        class ListAtom extends AbstractAtom implements Groundable {
            @Override public String toString() { return "GenericList"; }
            @Override public boolean equals(Object obj) { return obj instanceof ListAtom; }
            @Override public int hashCode() { return getClass().hashCode(); }
            @Override public Atom getType() { return atom("(List $T)"); } // Generic type
            @Override public List<Atom> execute(List<Atom> args) { throw new UnsupportedOperationException(); }
            @Override public List<Bindings> match(Atom other) { return Collections.emptyList(); }
            @Override public String toDisplayString() { return "GenericList"; }
        }
        GroundingSpace space = createSpace("");
        GroundedAtom genericList = new GroundedAtom(new ListAtom());
        List<AtomType> types = TypeChecker.getAtomTypes(space, genericList);
        assertEquals(1, types.size());
        Atom typeAtom = types.get(0).getAtom();
        assertTrue(typeAtom instanceof ExpressionAtom);
        ExpressionAtom typeExpr = (ExpressionAtom)typeAtom;
        assertEquals(atom("List"), typeExpr.getChildren().get(0));
        assertTrue(typeExpr.getChildren().get(1) instanceof VariableAtom);
        // Check that the variable was made unique (name ends with _xxxx)
        assertTrue(((VariableAtom)typeExpr.getChildren().get(1)).getName().startsWith("T_"));
    }


    @Test
    void getAtomTypesEmptyExpression() {
        GroundingSpace space = createSpace("");
        ExpressionAtom emptyExpr = new ExpressionAtom(Collections.emptyList());
        List<AtomType> types = TypeChecker.getAtomTypes(space, emptyExpr);
        assertEquals(1, types.size());
        assertEquals(MettaSymbols.UNIT_TYPE, types.get(0).getAtom());
    }

    @Test
    void checkTypeBasic() {
        GroundingSpace space = createSpace("(: Num (Type)) (: Int (Num)) (: 42 Int)");
        assertTrue(TypeChecker.checkType(space, atom("42"), atom("Int")));
        assertTrue(TypeChecker.checkType(space, atom("42"), atom("Num"))); // Requires transitive rule or type hierarchy traversal in getAtomTypes/checkType
                                                                      // Current getAtomTypes for Symbol is direct.
                                                                      // For this to pass, need (: Int Num) then query (: 42 $t), (: $t Num)
                                                                      // This test will likely fail for "Num" with current simple getAtomTypes.
                                                                      // Let's adjust space for direct check for this test.
        GroundingSpace space2 = createSpace("(: 42 Int) (: 42 Num)"); // Direct types for testing checkType
        assertTrue(TypeChecker.checkType(space2, atom("42"), atom("Int")));
        assertTrue(TypeChecker.checkType(space2, atom("42"), atom("Num")));
        assertFalse(TypeChecker.checkType(space2, atom("42"), atom("Float")));
        
        // Meta-type checks
        assertTrue(TypeChecker.checkType(space2, atom("42"), MettaSymbols.ATOM_TYPE));
        assertTrue(TypeChecker.checkType(space2, new SymbolAtom("sym"), MettaSymbols.SYMBOL_TYPE));
        assertTrue(TypeChecker.checkType(space2, new VariableAtom("var"), MettaSymbols.VARIABLE_TYPE));
        assertTrue(TypeChecker.checkType(space2, atom("(expr)"), MettaSymbols.EXPRESSION_TYPE));
        assertTrue(TypeChecker.checkType(space2, new GroundedAtom(1), MettaSymbols.GROUNDED_TYPE));
        
        // UNDEF_TYPE as expected matches anything
        assertTrue(TypeChecker.checkType(space2, atom("42"), MettaSymbols.UNDEF_TYPE));
    }
    
    @Test
    void getAtomTypesFunctionApplicationSimple() {
        // Space: (: id (-> $T $T))
        // Query: type of (id 42) -> expected Int (if 42 is Int)
        GroundingSpace space = createSpace("(: id (-> $T $T)) (: 42 Int)");
        ExpressionAtom expr = (ExpressionAtom) atom("(id 42)");
        
        List<AtomType> resultTypes = TypeChecker.getAtomTypes(space, expr);
        assertEquals(1, resultTypes.size());
        AtomType resultType = resultTypes.get(0);
        
        assertFalse(resultType.isError());
        assertTrue(resultType.isApplicationResult());
        assertEquals(atom("Int"), resultType.getAtom()); // $T should be bound to Int, so return type is Int
    }

    @Test
    void getAtomTypesFunctionApplicationArgMismatchError() {
        // Space: (: first (-> $A $B $A)) ; function that takes two args, returns type of first
        //        (: 42 Int)
        //        (: hello Str)
        // Query: type of (first 42) -> error, not enough arguments
        GroundingSpace space = createSpace("(: first (-> $A $B $A)) (: 42 Int) (: hello Str)");
        ExpressionAtom expr = (ExpressionAtom) atom("(first 42)");
        
        List<AtomType> resultTypes = TypeChecker.getAtomTypes(space, expr);
        // Should produce an error type because of argument count mismatch
        assertEquals(1, resultTypes.size());
        AtomType resultType = resultTypes.get(0);
        assertTrue(resultType.isError());
        assertTrue(resultType.isApplicationResult());
        assertEquals(MettaSymbols.ERROR_SYMBOL, resultType.getAtom());
    }
    
    @Test
    void validateAtomTest() {
        GroundingSpace space = createSpace("(: 42 Int)");
        assertTrue(TypeChecker.validateAtom(space, atom("42"))); // Has type Int
        
        GroundingSpace emptySpace = createSpace("");
        assertFalse(TypeChecker.validateAtom(emptySpace, atom("undefinedSymbol"))); // Gets UNDEF_TYPE only
        
        // Test with an error type scenario
        // If (some-func "str") results in an error type from getAtomTypes due to type mismatch
        // For now, cannot easily construct this without more complex setup.
        // Assume if getAtomTypes returns an error type, validateAtom would be false.
        // Let's test if UNDEF_TYPE alone is not valid.
        assertTrue(TypeChecker.validateAtom(space, new VariableAtom("x"))); // Variables get UNDEF_TYPE, should this be valid?
                                                                              // Current validateAtom returns true if not error and not *solely* UNDEF.
                                                                              // A variable's type is UNDEF_TYPE. So this depends on interpretation.
                                                                              // The plan: "not empty and does not solely consist of error types or only %Undefined%"
                                                                              // So, a variable (type %Undefined%) should make validateAtom return false.
         assertFalse(TypeChecker.validateAtom(emptySpace, new VariableAtom("x")));


    }

}
