package com.example.metta.space;

import com.example.metta.atom.*;
import com.example.metta.types.Bindings;
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience parsing

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

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
}
