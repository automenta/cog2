package com.example.metta.types;

import com.example.metta.atom.Atom;
import com.example.metta.atom.SymbolAtom;
import com.example.metta.atom.VariableAtom;
import com.example.metta.atom.ExpressionAtom; // For testing binding to expressions
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class BindingsTest {

    private final VariableAtom x = new VariableAtom("x");
    private final VariableAtom y = new VariableAtom("y");
    private final VariableAtom z = new VariableAtom("z");
    private final SymbolAtom a = new SymbolAtom("A");
    private final SymbolAtom b = new SymbolAtom("B");
    private final ExpressionAtom exprA = new ExpressionAtom(List.of(new SymbolAtom("f"), a));


    @Test
    void addValueBindingAndResolve() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addValueBinding(x, a));
        assertEquals(a, bindings.resolve(x));
        assertNull(bindings.resolve(y)); // y is not bound

        // Test binding to an expression
        Bindings bindings2 = new Bindings();
        assertTrue(bindings2.addValueBinding(y, exprA));
        assertEquals(exprA, bindings2.resolve(y));
    }

    @Test
    void addValueBindingConflict() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addValueBinding(x, a));
        assertFalse(bindings.addValueBinding(x, b)); // x already bound to A, cannot bind to B
        assertEquals(a, bindings.resolve(x)); // Should still be A
    }

    @Test
    void addVariableEquality() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addVariableEquality(x, y));
        
        // x and y should be in the same set, resolve(x) might return x or y (representative)
        // For now, let's check by binding one and resolving the other.
        assertTrue(bindings.addValueBinding(x, a));
        assertEquals(a, bindings.resolve(y));
        assertEquals(a, bindings.resolve(x));

        // Add another equality: y = z
        assertTrue(bindings.addVariableEquality(y, z));
        assertEquals(a, bindings.resolve(z));

        // Add equality with a bound variable
        Bindings bindings2 = new Bindings();
        assertTrue(bindings2.addValueBinding(x, b));
        assertTrue(bindings2.addVariableEquality(x, y)); // y should now also resolve to b
        assertEquals(b, bindings2.resolve(y));
    }

    @Test
    void addVariableEqualityConflict() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addValueBinding(x, a));
        assertTrue(bindings.addValueBinding(y, b));
        assertFalse(bindings.addVariableEquality(x, y)); // Conflict: x=A, y=B
    }
    
    @Test
    void addVariableEqualityTransitiveConflict() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addValueBinding(x, a));
        assertTrue(bindings.addValueBinding(z, b));
        assertTrue(bindings.addVariableEquality(x,y)); // x=y, so y is now 'a'
        assertFalse(bindings.addVariableEquality(y,z)); // y ('a') cannot be equal to z ('b')
    }


    @Test
    void resolveWithChainedVariables() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addVariableEquality(x, y));
        assertTrue(bindings.addVariableEquality(y, z));
        assertTrue(bindings.addValueBinding(z, a));

        assertEquals(a, bindings.resolve(x));
        assertEquals(a, bindings.resolve(y));
        assertEquals(a, bindings.resolve(z));
    }

    @Test
    void resolveLoopDetection() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.addVariableEquality(x, y));
        assertTrue(bindings.addVariableEquality(y, x)); // Creates a direct loop in DSU parent if not careful, but find handles path compression.
                                                      // More direct test: $x = $y, $y = $z, $z = $x
        
        Bindings loopBindings = new Bindings();
        assertTrue(loopBindings.addValueBinding(x, y)); // x := y
        assertTrue(loopBindings.addValueBinding(y, z)); // y := z  (so x := z)
        assertTrue(loopBindings.addValueBinding(z, x)); // z := x  (so x := x loop)

        // Depending on loop detection strategy in resolve (return null or var itself):
        // The current resolve returns the variable itself if a loop is detected during path formation.
        Atom resolvedX = loopBindings.resolve(x);
        assertTrue(resolvedX instanceof VariableAtom); // Should be x, y or z
        // To verify it's part of the loop:
        Set<VariableAtom> loopVars = Set.of(x,y,z);
        assertTrue(loopVars.contains((VariableAtom)resolvedX));

        // Test hasLoop() method
        assertTrue(loopBindings.hasLoop());

        Bindings nonLoopBindings = new Bindings();
        nonLoopBindings.addValueBinding(x,a);
        nonLoopBindings.addValueBinding(y,x);
        assertFalse(nonLoopBindings.hasLoop()); // y resolves to A via x.
        assertEquals(a, nonLoopBindings.resolve(y));

    }

    @Test
    void mergeBindingsNoConflict() {
        Bindings b1 = new Bindings();
        b1.addValueBinding(x, a); // x=A

        Bindings b2 = new Bindings();
        b2.addValueBinding(y, b); // y=B

        List<Bindings> mergedList = b1.merge(b2);
        assertEquals(1, mergedList.size());
        Bindings merged = mergedList.get(0);

        assertEquals(a, merged.resolve(x));
        assertEquals(b, merged.resolve(y));
    }

    @Test
    void mergeBindingsWithOverlapNoConflict() {
        Bindings b1 = new Bindings();
        b1.addValueBinding(x, a);    // x=A
        b1.addVariableEquality(y, z); // y=z

        Bindings b2 = new Bindings();
        b2.addValueBinding(x, a);    // x=A (consistent)
        b2.addValueBinding(y, b);    // y=B (so z=B)

        List<Bindings> mergedList = b1.merge(b2);
        assertEquals(1, mergedList.size());
        Bindings merged = mergedList.get(0);
        
        assertEquals(a, merged.resolve(x));
        assertEquals(b, merged.resolve(y));
        assertEquals(b, merged.resolve(z)); // Check transitive equality from y=z in b1
    }
    
    @Test
    void mergeBindingsWithVariableToValueAndEquality() {
        Bindings b1 = new Bindings();
        b1.addValueBinding(x, y); // x = $y

        Bindings b2 = new Bindings();
        b2.addValueBinding(y, a); // y = A
        
        List<Bindings> mergedList12 = b1.merge(b2);
        assertEquals(1, mergedList12.size());
        Bindings merged12 = mergedList12.get(0);
        assertEquals(a, merged12.resolve(x)); // x should resolve to A
        assertEquals(a, merged12.resolve(y));

        Bindings b3 = new Bindings();
        b3.addValueBinding(y, a); // y = A

        Bindings b4 = new Bindings();
        b4.addValueBinding(x, y); // x = $y
        
        List<Bindings> mergedList34 = b3.merge(b4);
        assertEquals(1, mergedList34.size());
        Bindings merged34 = mergedList34.get(0);
        assertEquals(a, merged34.resolve(x));
        assertEquals(a, merged34.resolve(y));
    }


    @Test
    void mergeBindingsConflict() {
        Bindings b1 = new Bindings();
        b1.addValueBinding(x, a); // x=A

        Bindings b2 = new Bindings();
        b2.addValueBinding(x, b); // x=B (conflict)

        List<Bindings> mergedList = b1.merge(b2);
        assertTrue(mergedList.isEmpty());
    }

    @Test
    void isEmptyAndCopy() {
        Bindings bindings = new Bindings();
        assertTrue(bindings.isEmpty());

        bindings.addValueBinding(x, a);
        assertFalse(bindings.isEmpty());

        Bindings copy = bindings.copy();
        assertFalse(copy.isEmpty());
        assertEquals(a, copy.resolve(x));

        // Ensure copy is deep for valueBindings and parent map
        copy.addValueBinding(y,b);
        assertNull(bindings.resolve(y)); // Original should not be affected
        
        bindings.addVariableEquality(x,z); // z=A
        assertNull(copy.resolve(z)); // Copy should not have z=A unless explicitly added to copy
                                     // This check is tricky due to DSU's shared nature if not careful.
                                     // Current copy creates new HashMaps, so it should be fine.
                                     // Re-check: find(z) on `copy` won't know about `z` from `bindings`
                                     // unless `z` was also part of an operation in `copy`.
                                     // If `z` is added to copy, it should be independent.
        VariableAtom z_copy = new VariableAtom("z"); // Ensure it's a "new" var for copy context
        assertNull(copy.resolve(z_copy)); // z_copy is unbound in copy
        copy.addVariableEquality(x, z_copy); // in copy: x=z_copy, and x=a, so z_copy=a
        assertEquals(a, copy.resolve(z_copy));
        
        // Original bindings: x=a, x=z -> z=a
        // We resolve z in original bindings, not z_copy
        assertEquals(a, bindings.resolve(z)); 
    }
    
    @Test
    void toStringNotEmpty() {
        Bindings bindings = new Bindings();
        bindings.addValueBinding(x,a);
        bindings.addVariableEquality(y,z);
        bindings.addValueBinding(z,b); // y=z=B
        String str = bindings.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
        // System.out.println(str); // For manual inspection if needed
        assertTrue(str.contains("$x := A"));
        assertTrue(str.contains("$y")); // y might be shown mapped to z or B
        assertTrue(str.contains("$z := B"));
    }
}
