package org.opencog.hyperon.atoms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

public class GroundedAtomTest {

    // A simple mockable Groundable implementation for testing
    static class TestOperation implements Groundable {
        private final String id;
        public TestOperation(String id) { this.id = id; }
        @Override
        public Atom execute(List<Atom> args) {
            // For testing, just return a symbol with the concatenated names of args
            // or a specific symbol if no args.
            if (args == null || args.isEmpty()) {
                return new SymbolAtom("executed_" + id);
            }
            StringBuilder resultName = new StringBuilder("executed_");
            for (Atom arg : args) {
                try {
                    resultName.append(arg.getName());
                } catch (UnsupportedOperationException e) {
                    resultName.append("expr");
                }
            }
            return new SymbolAtom(resultName.toString());
        }
        // Implement equals and hashCode for reliable use in GroundedAtom.equals/hashCode if needed
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestOperation that = (TestOperation) obj;
            return id.equals(that.id);
        }
        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    static class AnotherTestOperation implements Groundable {
        @Override
        public Atom execute(List<Atom> args) {
            return new SymbolAtom("another_executed");
        }
    }


    @Test
    void testGroundedAtomCreation() {
        Groundable op = new TestOperation("op1");
        GroundedAtom atom = new GroundedAtom("testOp", op);

        assertEquals("testOp", atom.getName());
        assertEquals(Atom.AtomType.GROUNDED, atom.getType());
        assertSame(op, atom.getOperation());
        assertTrue(atom.getChildren().isEmpty());
    }

    @Test
    void testGroundedAtomCreationNullName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GroundedAtom(null, new TestOperation("op1")));
        assertEquals("GroundedAtom name cannot be null or empty.", ex.getMessage());
    }

    @Test
    void testGroundedAtomCreationEmptyName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GroundedAtom("  ", new TestOperation("op1")));
        assertEquals("GroundedAtom name cannot be null or empty.", ex.getMessage());
    }


    @Test
    void testGroundedAtomCreationNullOperation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GroundedAtom("testOp", null));
        assertEquals("GroundedAtom operation cannot be null.", ex.getMessage());
    }

    @Test
    void testExecuteOperation() {
        Groundable mockOp = mock(Groundable.class);
        SymbolAtom expectedResult = new SymbolAtom("result");
        List<Atom> args = Collections.singletonList(new SymbolAtom("arg1"));

        when(mockOp.execute(args)).thenReturn(expectedResult);

        GroundedAtom atom = new GroundedAtom("execOp", mockOp);
        Atom actualResult = atom.executeOperation(args);

        assertSame(expectedResult, actualResult);
        verify(mockOp, times(1)).execute(args);
    }

    @Test
    void testExecuteOperationWithTestImpl() {
        TestOperation op = new TestOperation("op1");
        GroundedAtom atom = new GroundedAtom("myOp", op);

        Atom result1 = atom.executeOperation(Collections.emptyList());
        assertEquals(new SymbolAtom("executed_op1"), result1);

        List<Atom> args = List.of(new SymbolAtom("A"), new VariableAtom("$V"));
        Atom result2 = atom.executeOperation(args);
        assertEquals(new SymbolAtom("executed_A$V"), result2);
    }


    @Test
    void testEqualsAndHashCode() {
        Groundable op1 = new TestOperation("op1");
        Groundable op1b = new TestOperation("op1"); // Same content, different instance
        Groundable op2 = new TestOperation("op2");
        Groundable anotherOp = new AnotherTestOperation();


        GroundedAtom atom1 = new GroundedAtom("testOp", op1);
        GroundedAtom atom1Dup = new GroundedAtom("testOp", op1); // Same name, same op instance
        GroundedAtom atom1b = new GroundedAtom("testOp", op1b); // Same name, different op instance but op.equals() is true
                                                               // However, GroundedAtom.equals uses op.getClass()
        GroundedAtom atom2 = new GroundedAtom("testOp", op2); // Same name, different op content
        GroundedAtom atom3 = new GroundedAtom("otherOp", op1); // Different name, same op
        GroundedAtom atom4 = new GroundedAtom("testOp", anotherOp); // Same name, different op class

        // Reflexive
        assertEquals(atom1, atom1);

        // Symmetric & Consistent
        assertEquals(atom1, atom1Dup);
        assertEquals(atom1Dup, atom1);
        assertEquals(atom1.hashCode(), atom1Dup.hashCode());

        // Same name, op.getClass() is the same
        assertEquals(atom1, atom1b);
        assertEquals(atom1.hashCode(), atom1b.hashCode());

        // Same name, op.getClass() is the same, but op1 and op2 are not equal (if op.equals was used)
        // Since GroundedAtom.equals uses op.getClass(), and TestOperation.equals is based on id,
        // atom1 and atom2 will be equal if their names are the same and op classes are the same.
        // The current implementation of GroundedAtom.equals uses operation.getClass().
        // So atom1 and atom2 should be equal.
        assertEquals(atom1, atom2, "Atoms with same name and op class should be equal");
        assertEquals(atom1.hashCode(), atom2.hashCode());


        // Different name
        assertNotEquals(atom1, atom3);

        // Different operation class
        assertNotEquals(atom1, atom4);

        // Null
        assertNotEquals(atom1, null);

        // Different type
        assertNotEquals(atom1, new SymbolAtom("testOp"));
    }

    @Test
    void testToString() {
        Groundable op = new TestOperation("opToString");
        GroundedAtom atom = new GroundedAtom("myToStringOp", op);
        assertEquals("myToStringOp", atom.toString());
    }
}
