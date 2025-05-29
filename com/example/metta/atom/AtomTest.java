package com.example.metta.atom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public class AtomTest {

    @Test
    void symbolAtomEqualityAndHashCode() {
        SymbolAtom sym1 = new SymbolAtom("test");
        SymbolAtom sym2 = new SymbolAtom("test");
        SymbolAtom sym3 = new SymbolAtom("different");

        assertEquals(sym1, sym2);
        assertNotEquals(sym1, sym3);
        assertEquals(sym1.hashCode(), sym2.hashCode());
        assertNotEquals(sym1.hashCode(), sym3.hashCode());
        assertEquals("test", sym1.toString());
    }

    @Test
    void variableAtomEqualityAndHashCode() {
        VariableAtom var1 = new VariableAtom("x");
        VariableAtom var2 = new VariableAtom("x");
        VariableAtom var3 = new VariableAtom("y");

        assertEquals(var1, var2);
        assertNotEquals(var1, var3);
        assertEquals(var1.hashCode(), var2.hashCode());
        assertNotEquals(var1.hashCode(), var3.hashCode());
        assertEquals("$x", var1.toString());
    }

    @Test
    void expressionAtomEqualityAndHashCode() {
        ExpressionAtom expr1 = new ExpressionAtom(List.of(new SymbolAtom("f"), new VariableAtom("x")));
        ExpressionAtom expr2 = new ExpressionAtom(List.of(new SymbolAtom("f"), new VariableAtom("x")));
        ExpressionAtom expr3 = new ExpressionAtom(List.of(new SymbolAtom("g"), new VariableAtom("x")));
        ExpressionAtom expr4 = new ExpressionAtom(List.of(new SymbolAtom("f"), new VariableAtom("y")));
        ExpressionAtom emptyExpr = new ExpressionAtom(Collections.emptyList());

        assertEquals(expr1, expr2);
        assertNotEquals(expr1, expr3);
        assertNotEquals(expr1, expr4);
        assertEquals(expr1.hashCode(), expr2.hashCode());
        assertNotEquals(expr1.hashCode(), expr3.hashCode());

        assertEquals("(f $x)", expr1.toString());
        assertEquals("()", emptyExpr.toString());
    }
    
    @Test
    void expressionAtomImmutability() {
        List<Atom> children = new ArrayList<>(Arrays.asList(new SymbolAtom("a"), new SymbolAtom("b")));
        ExpressionAtom expr = new ExpressionAtom(children);
        
        // Try to modify the original list
        children.add(new SymbolAtom("c"));
        
        // Check if the expression atom's children remain unchanged
        assertEquals(2, expr.getChildren().size());
        assertEquals(new SymbolAtom("a"), expr.getChildren().get(0));
        assertEquals(new SymbolAtom("b"), expr.getChildren().get(1));

        // Try to modify the list returned by getChildren()
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            expr.getChildren().add(new SymbolAtom("d"));
        });
        assertNotNull(exception);
    }


    @Test
    void groundedAtomEqualityAndHashCode() {
        GroundedAtom ga1 = new GroundedAtom(42);
        GroundedAtom ga2 = new GroundedAtom(42);
        GroundedAtom ga3 = new GroundedAtom(100);
        GroundedAtom ga4 = new GroundedAtom("test");
        GroundedAtom ga5 = new GroundedAtom("test");

        assertEquals(ga1, ga2);
        assertNotEquals(ga1, ga3);
        assertNotEquals(ga1, ga4);
        assertEquals(ga4, ga5);
        
        assertEquals(ga1.hashCode(), ga2.hashCode());
        assertNotEquals(ga1.hashCode(), ga3.hashCode());
        assertEquals(ga4.hashCode(), ga5.hashCode());

        assertEquals("42", ga1.toString()); // DefaultGroundable uses value.toString()
        assertEquals("test", ga4.toString());
    }
    
    @Test
    void groundedAtomCustomGroundable() {
        class MyVal {
            int x;
            MyVal(int x) { this.x = x; }
            @Override public boolean equals(Object o) { return (o instanceof MyVal) && ((MyVal)o).x == x; }
            @Override public int hashCode() { return x; }
            @Override public String toString() { return "MyVal:"+x; }
        }
        class MyGroundable implements Groundable {
            MyVal val;
            MyGroundable(MyVal val) { this.val = val; }
            @Override public Atom getType() { return new SymbolAtom("MyValType"); }
            @Override public List<Atom> execute(List<Atom> args) { throw new UnsupportedOperationException(); }
            @Override public List<com.example.metta.types.Bindings> match(Atom other) { return Collections.emptyList(); }
            @Override public String toDisplayString() { return "CustomDisplay("+val.x+")"; }
        }

        MyVal obj1 = new MyVal(10);
        MyGroundable groundable1 = new MyGroundable(obj1);
        GroundedAtom gaCustom = new GroundedAtom(obj1, groundable1);
        
        assertEquals("CustomDisplay(10)", gaCustom.toString());
        assertEquals(new SymbolAtom("MyValType"), gaCustom.getType());

        GroundedAtom gaDefault = new GroundedAtom(obj1); // Uses DefaultGroundable
        assertEquals("MyVal:10", gaDefault.toString()); // DefaultGroundable uses obj.toString()
        assertTrue(gaDefault.getType().toString().contains("MyVal")); // Default type is Java.ClassName
    }

    @Test
    void nestedExpressionToString() {
        ExpressionAtom inner = new ExpressionAtom(List.of(new SymbolAtom("b"), new VariableAtom("y")));
        ExpressionAtom outer = new ExpressionAtom(List.of(new SymbolAtom("a"), inner, new SymbolAtom("c")));
        assertEquals("(a (b $y) c)", outer.toString());
    }
}
