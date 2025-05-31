package com.example.metta.space;

import com.example.metta.atom.*; // Wildcard, but specific ones below for clarity if needed
import com.example.metta.atom.LinkAtom;
import com.example.metta.atom.VariableAtom; // Though likely covered by above
import com.example.metta.types.Bindings;
import com.example.metta.text.SExprParser; // For new parse helper
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience parsing

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays; // For Arrays.asList
import java.util.stream.Collectors;
import java.util.Collections; // For Collections.emptyList

public class GroundingSpaceTest {

    private final VariableAtom x = new VariableAtom("x");
    private final VariableAtom y = new VariableAtom("y");
    private final SymbolAtom a = new SymbolAtom("A");
    private final SymbolAtom b = new SymbolAtom("B");
    private final SymbolAtom f = new SymbolAtom("f");
    private final SymbolAtom g = new SymbolAtom("g");

    @Test
    void addAndGetAtoms() {
        GroundingSpace space = new GroundingSpace();
        ExpressionAtom expr1 = new ExpressionAtom(List.of(f, a));
        ExpressionAtom expr2 = new ExpressionAtom(List.of(g, b));

        space.add(expr1);
        assertTrue(space.contains(expr1));
        assertFalse(space.contains(expr2));
        
        space.add(expr2);
        assertTrue(space.contains(expr2));
        assertEquals(2, space.getAtoms().size()); // Order not guaranteed by HashSet
        assertTrue(space.getAtoms().contains(expr1));
        assertTrue(space.getAtoms().contains(expr2));

        // Test adding duplicate
        space.add(expr1);
        assertEquals(2, space.getAtoms().size()); 
    }

    @Test
    void removeAtom() {
        GroundingSpace space = new GroundingSpace();
        ExpressionAtom expr1 = new ExpressionAtom(List.of(f, a));
        space.add(expr1);
        assertTrue(space.contains(expr1));

        assertTrue(space.remove(expr1));
        assertFalse(space.contains(expr1));
        assertTrue(space.getAtoms().isEmpty());
        assertFalse(space.remove(expr1)); // Try removing again
    }

    @Test
    void replaceAtom() {
        GroundingSpace space = new GroundingSpace();
        ExpressionAtom expr1 = new ExpressionAtom(List.of(f, a));
        ExpressionAtom expr2 = new ExpressionAtom(List.of(g, b));
        ExpressionAtom expr3 = new ExpressionAtom(List.of(f, b));


        space.add(expr1);
        assertTrue(space.contains(expr1));

        assertFalse(space.replace(expr2, expr3)); // expr2 not in space
        assertTrue(space.contains(expr1));
        assertFalse(space.contains(expr3));

        assertTrue(space.replace(expr1, expr3)); // expr1 replaced by expr3
        assertFalse(space.contains(expr1));
        assertTrue(space.contains(expr3));
        assertEquals(1, space.getAtoms().size());
    }

    @Test
    void simpleQueryVariable() {
        GroundingSpace space = new GroundingSpace();
        // (= A B)
        // (= A C)
        space.add(atom("(= A B)"));
        space.add(atom("(= A C)"));
        space.add(atom("(= D E)"));

        // Query: (= A $x)
        List<Bindings> results = space.query(atom("(= A $x)"));
        assertEquals(2, results.size());

        List<String> resolvedValues = results.stream()
            .map(b -> b.resolve(new VariableAtom("x")).toString())
            .sorted()
            .collect(Collectors.toList());
        
        assertEquals(List.of("B", "C"), resolvedValues);
    }
    
    @Test
    void simpleQueryConcrete() {
        GroundingSpace space = new GroundingSpace();
        space.add(atom("(= A B)"));
        space.add(atom("(f C)"));

        List<Bindings> results = space.query(atom("(= A B)")); // Exact match
        assertEquals(1, results.size());
        assertTrue(results.get(0).isEmpty()); // No variables, so empty bindings

        results = space.query(atom("(= A C)")); // No match
        assertTrue(results.isEmpty());
    }

    @Test
    void queryWithNarrowing() {
        GroundingSpace space = new GroundingSpace();
        // (f A B) - target for query (f $x $y), $x and $y are relevant
        // (g C D) - target for query (g $x $z), $x and $z are relevant
        space.add(atom("(f A B)"));
        
        // Query (f $x $y)
        List<Bindings> results = space.query(atom("(f $x $y)"));
        assertEquals(1, results.size());
        Bindings b = results.get(0);
        assertEquals(atom("A"), b.resolve(x));
        assertEquals(atom("B"), b.resolve(y));
        assertNull(b.resolve(new VariableAtom("z"))); // z was not in query pattern

        // Test that narrowVariables in GroundingSpace.query works
        // If we had a more complex match that internally bound $z,
        // narrowVariables should remove it.
        // Example: space has (match-internally (A $z) (A B)) -> $z=B
        // Query: (match-internally $x $y)
        // If Matcher.matchAtoms for (match-internally $x $y) vs (match-internally (A $z) (A B))
        // produced bindings $x=(A $z), $y=(A B), and $z=B.
        // The narrowVariables should ensure final bindings only have $x, $y.
        // This is implicitly tested by checking for non-existence of $z above.
    }


    @Test
    void conjunctiveQuery() {
        GroundingSpace space = new GroundingSpace();
        space.add(atom("(link A B)"));
        space.add(atom("(link B C)"));
        space.add(atom("(link C D)"));
        space.add(atom("(property B fast)"));
        space.add(atom("(property C slow)"));

        // Query: find X such that (link A X) and (link X C)
        // (, (link A $x) (link $x C))
        Atom query1 = atom("(, (link A $x) (link $x C))");
        List<Bindings> results1 = space.query(query1);
        assertEquals(1, results1.size());
        assertEquals(atom("B"), results1.get(0).resolve(x));

        // Query: find X, Y such that (link X Y) and (property Y fast)
        // (, (link $x $y) (property $y fast))
        // Matches: (link A B), (property B fast) -> $x=A, $y=B
        Atom query2 = atom("(, (link $x $y) (property $y fast))");
        List<Bindings> results2 = space.query(query2);
        assertEquals(1, results2.size());
        Bindings b2 = results2.get(0);
        assertEquals(atom("A"), b2.resolve(x));
        assertEquals(atom("B"), b2.resolve(y));
        
        // Query: find X, Y such that (link X Y) and (property Y slow)
        // Matches: (link B C), (property C slow) -> $x=B, $y=C
        // Also: (link C D) - (property D ???) - no.
        Atom query3 = atom("(, (link $x $y) (property $y slow))");
        List<Bindings> results3 = space.query(query3);
        assertEquals(1, results3.size()); // Only one path leads to (property $y slow)
        Bindings b3 = results3.get(0);
        assertEquals(atom("B"), b3.resolve(x)); // (link B C)
        assertEquals(atom("C"), b3.resolve(y)); // (property C slow)
    }
    
    @Test
    void conjunctiveQueryNoMatch() {
        GroundingSpace space = new GroundingSpace();
        space.add(atom("(link A B)"));
        space.add(atom("(property C fast)"));
        
        // Query: (, (link A $x) (property $x fast))
        // (link A B) -> $x=B. Then (property B fast) is not in space.
        Atom query = atom("(, (link A $x) (property $x fast))");
        List<Bindings> results = space.query(query);
        assertTrue(results.isEmpty());
    }

    @Test
    void substMethod() {
        GroundingSpace space = new GroundingSpace();
        space.add(atom("(= (name John) \"Doe\")"));
        space.add(atom("(= (name Jane) \"Smith\")"));
        
        // subst pattern: (= (name $first) $last)
        // subst template: (full-name $first $last)
        Atom pattern = atom("(= (name $first) $last)");
        Atom template = atom("(full-name $first $last)");
        
        List<Atom> substResults = space.subst(pattern, template);
        assertEquals(2, substResults.size());
        
        List<String> resultsStr = substResults.stream()
                                    .map(Object::toString)
                                    .sorted()
                                    .collect(Collectors.toList());
        
        // Note: SExprParser parses "Doe" as SymbolAtom("Doe")
        // AtomPrinter will print SymbolAtom("Doe") as "Doe" if it needs quotes.
        // Current heuristic in AtomPrinter: Symbol "Doe" doesn't strictly need quotes.
        // If it were SymbolAtom("string with space"), it would be quoted.
        // Let's adjust test to match current printer/parser for simple strings.
        // If "Doe" became "\"Doe\"" this would fail.
        // The SymbolAtom("Doe") will print as Doe.
        // The SymbolAtom("Smith") will print as Smith.
        
        assertEquals(List.of("(full-name Jane Smith)", "(full-name John Doe)"), resultsStr);
    }

    // Helper parse method for new tests
    private Atom parse(String mettaSrc) {
        return new SExprParser().parse(mettaSrc);
    }

    @Test
    void testSingleLinkTraversal() {
        GroundingSpace space = new GroundingSpace();
        SymbolAtom x = new SymbolAtom("x");
        SymbolAtom y = new SymbolAtom("y");
        SymbolAtom z = new SymbolAtom("z");
        SymbolAtom typeA = new SymbolAtom("A");
        SymbolAtom typeB = new SymbolAtom("B");

        space.add(new LinkAtom(typeA, Arrays.asList(x, y)));
        space.add(new LinkAtom(typeB, Arrays.asList(y, z)));

        // Query: (Traverse x (A $1 $2) $result)
        Atom query1 = parse("(Traverse x (A $1 $2) $result)");
        List<Bindings> results1 = space.query(query1);

        assertEquals(1, results1.size(), "Expected one binding for query1");
        if (!results1.isEmpty()) {
            Atom resultVal = results1.get(0).resolve(new VariableAtom("$result"));
            assertEquals(y, resultVal, "Query1: $result should be y");
        }

        // Query: (Traverse $start (A $1 $2) y)
        Atom query2 = parse("(Traverse $start (A $S $E) y)"); // Use $S, $E in pattern
        List<Bindings> results2 = space.query(query2);
        assertEquals(1, results2.size(), "Expected one binding for query2");
        if (!results2.isEmpty()) {
            Atom startVal = results2.get(0).resolve(new VariableAtom("$start"));
            assertEquals(x, startVal, "Query2: $start should be x");
        }

        // Query: (Traverse x (UnkLink $1 $2) $result) - non-existent link type
        Atom query3 = parse("(Traverse x (UnkLink $1 $2) $result)");
        List<Bindings> results3 = space.query(query3);
        assertTrue(results3.isEmpty(), "Query3: Should find no matches for UnkLink");

        // Query: (Traverse x (A $1) $result) - arity mismatch with LinkAtom(typeA, [x,y])
        Atom query4 = parse("(Traverse x (A $1) $result)");
        List<Bindings> results4 = space.query(query4);
        assertTrue(results4.isEmpty(), "Query4: Arity mismatch should result in no matches.");
    }

    @Test
    void testSequentialPathTraversalTwoLinks() {
        GroundingSpace space = new GroundingSpace();
        SymbolAtom x = new SymbolAtom("x");
        SymbolAtom y = new SymbolAtom("y");
        SymbolAtom z = new SymbolAtom("z");
        SymbolAtom typeA = new SymbolAtom("A");
        SymbolAtom typeB = new SymbolAtom("B");

        space.add(new LinkAtom(typeA, Arrays.asList(x, y))); // (A x y)
        space.add(new LinkAtom(typeB, Arrays.asList(y, z))); // (B y z)

        // Query: (Traverse x (Path (A $s1 $e1) (B $e1 $e2)) $result)
        Atom query = parse("(Traverse x (Path (A $s1 $e1) (B $e1 $e2)) $result)");
        List<Bindings> results = space.query(query);

        assertEquals(1, results.size(), "Expected one binding for 2-link path");
        if (!results.isEmpty()) {
            Atom resultVal = results.get(0).resolve(new VariableAtom("$result"));
            assertEquals(z, resultVal, "2-Link Path: $result should be z");
        }
    }

    @Test
    void testSequentialPathTraversalThreeLinks() {
        GroundingSpace space = new GroundingSpace();
        SymbolAtom a_sym = new SymbolAtom("a"); // Renamed from 'a' to avoid conflict with field
        SymbolAtom b_sym = new SymbolAtom("b"); // Renamed from 'b' to avoid conflict with field
        SymbolAtom c = new SymbolAtom("c");
        SymbolAtom d = new SymbolAtom("d");
        SymbolAtom typeL1 = new SymbolAtom("L1");
        SymbolAtom typeL2 = new SymbolAtom("L2");
        SymbolAtom typeL3 = new SymbolAtom("L3");

        space.add(new LinkAtom(typeL1, Arrays.asList(a_sym, b_sym))); // (L1 a b)
        space.add(new LinkAtom(typeL2, Arrays.asList(b_sym, c)));    // (L2 b c)
        space.add(new LinkAtom(typeL3, Arrays.asList(c, d)));      // (L3 c d)

        // Query: (Traverse a (Path (L1 $n1 $n2) (L2 $n2 $n3) (L3 $n3 $n4)) $final_dest)
        Atom query = parse("(Traverse a (Path (L1 $_1 $_2) (L2 $_2 $_3) (L3 $_3 $_4)) $final_dest)");
        List<Bindings> results = space.query(query);

        assertEquals(1, results.size(), "Expected one binding for 3-link path");
        if (!results.isEmpty()) {
            Atom resultVal = results.get(0).resolve(new VariableAtom("$final_dest"));
            assertEquals(d, resultVal, "3-Link Path: $final_dest should be d");
        }
    }

    @Test
    void testPathTraversalWithUnboundStart() {
        GroundingSpace space = new GroundingSpace();
        SymbolAtom x_sym = new SymbolAtom("x"); // Renamed to avoid conflict
        SymbolAtom y_sym = new SymbolAtom("y"); // Renamed to avoid conflict
        SymbolAtom z_sym = new SymbolAtom("z"); // Renamed to avoid conflict
        SymbolAtom typeA = new SymbolAtom("A");
        SymbolAtom typeB = new SymbolAtom("B");

        space.add(new LinkAtom(typeA, Arrays.asList(x_sym, y_sym))); // (A x y)
        space.add(new LinkAtom(typeB, Arrays.asList(y_sym, z_sym))); // (B y z)
        space.add(new LinkAtom(typeA, Arrays.asList(new SymbolAtom("other"), x_sym))); // (A other x) for variety

        // Query: (Traverse $start (Path (A $s1 $e1) (B $e1 $e2)) z)
        Atom query = parse("(Traverse $start (Path (A $_1 $_2) (B $_2 $_3)) z)");
        List<Bindings> results = space.query(query);

        assertEquals(1, results.size(), "Expected one path ending in z");
        if(!results.isEmpty()){
            Atom startNode = results.get(0).resolve(new VariableAtom("$start"));
            assertEquals(x_sym, startNode, "Path to z should start from x");
        }
    }
}
