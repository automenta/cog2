package com.example.metta.space;

import com.example.metta.atom.*; // Wildcard, but specific ones below for clarity if needed
import com.example.metta.atom.LinkAtom;
import com.example.metta.atom.MettaSymbols; // Added for PATH_STAR_SYMBOL
import com.example.metta.atom.VariableAtom; // Though likely covered by above
import com.example.metta.types.Bindings;
import com.example.metta.text.SExprParser; // For new parse helper
import static com.example.metta.testing.MettaTestUtils.atom; // For convenience parsing

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach; // Added for @BeforeEach
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays; // For Arrays.asList
import java.util.stream.Collectors;
import java.util.Collections; // For Collections.emptyList


public class GroundingSpaceTest {

    // Fields like x, y, a, b, f, g can be removed if tests define their own atoms locally,
    // or keep them if they are generally useful and don't conflict with local test vars.
    // For now, let them be.
    private final VariableAtom x_field = new VariableAtom("x"); // Renamed to avoid conflict
    private final VariableAtom y_field = new VariableAtom("y"); // Renamed to avoid conflict
    private final SymbolAtom a_field = new SymbolAtom("A");     // Renamed to avoid conflict
    private final SymbolAtom b_field = new SymbolAtom("B");     // Renamed to avoid conflict
    private final SymbolAtom f_field = new SymbolAtom("f");     // Renamed to avoid conflict
    private final SymbolAtom g_field = new SymbolAtom("g");     // Renamed to avoid conflict

    private GroundingSpace space;

    @BeforeEach
    void setUp() {
        space = new GroundingSpace();
    }

    // Helper methods for test case construction
    private SymbolAtom sym(String name) { return new SymbolAtom(name); }
    private VariableAtom var(String name) { return new VariableAtom(name); }
    private ExpressionAtom expr(Atom... children) { return new ExpressionAtom(List.of(children)); }
    // parse method already exists via SExprParser instance or MettaTestUtils.atom

    @Test
    void addAndGetAtoms() {
        // GroundingSpace space = new GroundingSpace(); // Now initialized in setUp
        ExpressionAtom expr1 = expr(f_field, a_field); // Using renamed fields
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

    @Test
    void testPathStar_simplePathFound() {
        space.add(expr(sym("Likes"), sym("Alice"), sym("Bob")));
        space.add(expr(sym("Likes"), sym("Bob"), sym("Charlie")));

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("Alice"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Likes"), var("$X"), var("$Y"))),
                          sym("Charlie"));

        List<Bindings> results = space.query(query);
        assertEquals(1, results.size());
        // For this query with concrete start/end, bindings list might be empty or map constants to themselves.
        // The important part is that a path was found.
    }

    @Test
    void testPathStar_longerPathFound() {
        space.add(expr(sym("Links"), sym("L1"), sym("L2")));
        space.add(expr(sym("Links"), sym("L2"), sym("L3")));
        space.add(expr(sym("Links"), sym("L3"), sym("L4")));

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("L1"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Links"), var("$A"), var("$B"))),
                          sym("L4"));

        List<Bindings> results = space.query(query);
        assertEquals(1, results.size());
    }

    @Test
    void testPathStar_noPathFound() {
        space.add(expr(sym("Likes"), sym("Alice"), sym("Bob")));
        space.add(expr(sym("Likes"), sym("Charlie"), sym("Dave")));

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("Alice"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Likes"), var("$X"), var("$Y"))),
                          sym("Dave"));

        List<Bindings> results = space.query(query);
        assertEquals(0, results.size());
    }

    @Test
    void testPathStar_variableStartNode() {
        space.add(expr(sym("Rel"), sym("R1"), sym("P1"), sym("P2")));
        space.add(expr(sym("Rel"), sym("R1"), sym("P2"), sym("P3")));
        space.add(expr(sym("Rel"), sym("R2"), sym("X1"), sym("X2"))); // Different relation type

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          var("$S"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Rel"), sym("R1"), var("$A"), var("$B"))),
                          sym("P3"));

        List<Bindings> results = space.query(query);
        assertEquals(1, results.size());
        if (!results.isEmpty()) {
            assertEquals(sym("P1"), results.get(0).resolve(var("$S")));
        }
    }

    @Test
    void testPathStar_variableEndNode() {
        space.add(expr(sym("Rel"), sym("R1"), sym("P1"), sym("P2")));
        space.add(expr(sym("Rel"), sym("R1"), sym("P2"), sym("P3")));
        // For a more complex scenario for $E:
        // space.add(expr(sym("Rel"), sym("R1"), sym("P1"), sym("PX"))); // P1 -> PX
        // space.add(expr(sym("Rel"), sym("R1"), sym("PX"), sym("P3"))); // PX -> P3 (another path to P3)

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("P1"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Rel"), sym("R1"), var("$A"), var("$B"))),
                          var("$E"));

        List<Bindings> results = space.query(query);

        // Expect P1 -> P2 -> P3. So $E should be P3.
        // If there were other paths from P1, $E could take other values.
        // For this specific dataset, only P3 is reachable via (Rel R1 ...) from P1.
        assertEquals(1, results.size());
        if (!results.isEmpty()) {
            assertEquals(sym("P3"), results.get(0).resolve(var("$E")));
        }
    }

    @Test
    void testPathStar_internalVariableResolution() {
        space.add(expr(sym("Link"), sym("A"), sym("B")));
        space.add(expr(sym("Link"), sym("B"), sym("C")));

        // Query 1: Concrete start and end. Internal variables $IX, $IY should not be in final bindings.
        Atom query1 = expr(MettaSymbols.TRAVERSE_SYMBOL,
                           sym("A"),
                           expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Link"), var("$IX"), var("$IY"))),
                           sym("C"));

        List<Bindings> results1 = space.query(query1);
        assertEquals(1, results1.size());
        if (!results1.isEmpty()) {
            // Overall relevant vars are from the main query (A, C - both concrete).
            // So, the binding should be empty or map A to A, C to C.
            // The current narrowVariables implementation might result in an empty binding.
            assertTrue(results1.get(0).isEmpty() ||
                       (results1.get(0).resolve(var("$IX")) == null && results1.get(0).resolve(var("$IY")) == null) );
        }

        // Query 2: Variable start and end. Internal variables $IX, $IY should not be in final bindings.
        // Final bindings should only contain $S and $E.
        Atom query2 = expr(MettaSymbols.TRAVERSE_SYMBOL,
                           var("$S"),
                           expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Link"), var("$IX"), var("$IY"))),
                           var("$E"));

        List<Bindings> results2 = space.query(query2);
        // This query is broad: (Link A B), (Link B C)
        // Path 1: A -> B ($S=A, $E=B)
        // Path 2: B -> C ($S=B, $E=C)
        // Path 3: A -> B -> C ($S=A, $E=C)
        // The BFS in GroundingSpace finds paths of increasing lengths.
        // It should find all distinct pairs of ($S, $E) connected by (Link $IX $IY)+
        // The current implementation of PathStar returns bindings when the endNodeConstraint is met.
        // For $S -> $E:
        // Depth 0: ($S, $S) if $S matches $E.
        // Depth 1: (A,B), (B,C)
        // Depth 2: (A,C)
        // So, 3 results are expected.
        assertEquals(3, results2.size());

        boolean foundAB = false;
        boolean foundBC = false;
        boolean foundAC = false;

        for (Bindings b : results2) {
            Atom sVal = b.resolve(var("$S"));
            Atom eVal = b.resolve(var("$E"));
            assertNull(b.resolve(var("$IX"))); // Internal vars should not be in final bindings
            assertNull(b.resolve(var("$IY")));

            if (sVal.equals(sym("A")) && eVal.equals(sym("B"))) foundAB = true;
            if (sVal.equals(sym("B")) && eVal.equals(sym("C"))) foundBC = true;
            if (sVal.equals(sym("A")) && eVal.equals(sym("C"))) foundAC = true;
        }
        assertTrue(foundAB, "Missing path A->B");
        assertTrue(foundBC, "Missing path B->C");
        assertTrue(foundAC, "Missing path A->C");
    }

    @Test
    void testPathStar_pathWithCycle_depthLimited() {
        space.add(expr(sym("Cycle"), sym("C1"), sym("C2")));
        space.add(expr(sym("Cycle"), sym("C2"), sym("C1"))); // Cycle C1 <-> C2
        space.add(expr(sym("Cycle"), sym("C2"), sym("C3"))); // Path out of cycle: C2 -> C3

        // Query: (traverse C1 (PathStar (Cycle $A $B)) C3)
        // Expected path: C1 -> C2 -> C3 (length 2)
        // MAX_PATH_STAR_DEPTH is 10, so this should be found and not hang.
        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("C1"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Cycle"), var("$A"), var("$B"))),
                          sym("C3"));

        List<Bindings> results = space.query(query);
        assertEquals(1, results.size(), "Expected one path C1->C2->C3");
        // If there were bindings to check, e.g., if C1 or C3 were variables:
        // Bindings b = results.get(0);
        // assertEquals(sym("C1"), b.resolve(var("$StartVar")));
        // assertEquals(sym("C3"), b.resolve(var("$EndVar")));
    }

    @Test
    void testPathStar_emptySpace() {
        // Space is already empty due to @BeforeEach setUp()
        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("A"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Likes"), var("$X"), var("$Y"))),
                          sym("B"));

        List<Bindings> results = space.query(query);
        assertEquals(0, results.size());
    }

    @Test
    void testPathStar_startEqualsEnd_directMatch() {
        space.add(sym("NodeA")); // Add the node to the space so it can be "found"

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("NodeA"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Links"), var("$X"), var("$Y"))),
                          sym("NodeA"));

        List<Bindings> results = space.query(query);
        // PathStar finds paths of length >= 0.
        // If startNode matches endNodeConstraint initially (depth 0), it's a valid path.
        assertEquals(1, results.size());
        if (!results.isEmpty()) {
            assertTrue(results.get(0).isEmpty()); // No variables to bind in this specific query
        }
    }

    @Test
    void testPathStar_startEqualsEnd_viaSelfLink() {
        space.add(expr(sym("Loop"), sym("L"), sym("L")));

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("L"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Loop"), var("$A"), var("$B"))),
                          sym("L"));

        List<Bindings> results = space.query(query);
        // Path 1: L matches L (depth 0)
        // Path 2: L -> L (via (Loop L L), depth 1)
        // The results should be distinct, so 2 results.
        assertEquals(2, results.size());

        // It's a bit tricky to assert distinctness of bindings if they are both empty.
        // However, the logic in PathStar adds to finalResults if endNodeConstraint matches.
        // 1. Initial state: currentNode=L, depth=0. Matches endNodeConstraint=L. Add Bindings().
        // 2. Step from L: via (Loop L L), nextNode=L, depth=1. Matches endNodeConstraint=L. Add Bindings().
        // The final .distinct() on the stream of bindings should ensure only one empty binding remains if they are identical.
        // Let's refine the expectation: if the bindings are truly identical (e.g. both empty), distinct will make it 1.
        // If the underlying mechanism of finding paths of different lengths inherently produces distinct
        // results or if the Bindings objects themselves are different instances even if semantically empty,
        // then it could be 2.
        // Given the current BFS, it finds L at depth 0, and then L again at depth 1.
        // If both produce an empty Bindings() and it's the same instance or value-equal, .distinct() would yield 1.
        // If they are different instances that are value-equal, .distinct() still yields 1.
        // This means we should expect 1 result if the bindings are truly identical.
        // Let's test this assumption. If it fails, we'll know more about how distinct bindings are handled.
        // The current code does `finalResults.add(merged.get(0));` where merged is a copy.
        // So they will be different instances of Bindings, but semantically empty.
        // Thus, .distinct() should yield 1.
        assertEquals(1, results.size(), "Expected 1 distinct binding (L->L self-match and direct L match resolve to same empty binding)");
         if (!results.isEmpty()) {
            assertTrue(results.get(0).isEmpty());
        }
    }

    @Test
    void testPathStar_multiplePaths_sameStartEnd() {
        space.add(expr(sym("Edge"), sym("A"), sym("X")));
        space.add(expr(sym("Edge"), sym("X"), sym("C"))); // Path 1: A -> X -> C
        space.add(expr(sym("Edge"), sym("A"), sym("Y")));
        space.add(expr(sym("Edge"), sym("Y"), sym("C"))); // Path 2: A -> Y -> C

        Atom query = expr(MettaSymbols.TRAVERSE_SYMBOL,
                          sym("A"),
                          expr(MettaSymbols.PATH_STAR_SYMBOL, expr(sym("Edge"), var("$F"), var("$T"))),
                          sym("C"));

        List<Bindings> results = space.query(query);
        // Both paths (A->X->C and A->Y->C) are of length 2.
        // The BFS explores layer by layer.
        // When C is reached at depth 2 from X, a binding is added.
        // When C is reached at depth 2 from Y, another binding is added.
        // These bindings, even if they are both "empty" because A and C are concrete,
        // are generated from different path explorations and added to `finalResults` before `distinct()`.
        // If the `Bindings` objects are distinct instances (even if semantically empty),
        // and `Bindings.equals/hashCode` consider them identical, `distinct()` would make it 1.
        // However, the path taken to C is different. The internal state of `currentPathBindings`
        // within `executePathStarTraversal` would be different when `endNodeConstraint` is checked for C via X versus via Y.
        // For example, if the query was `(Traverse $S (PathStar (Edge $F $T)) $E)`
        // Path 1: $S=A, $F=A, $T=X then $F=X, $T=C, $E=C. Bindings {$S:A, $E:C, $F:X, $T:C} (before narrowing)
        // Path 2: $S=A, $F=A, $T=Y then $F=Y, $T=C, $E=C. Bindings {$S:A, $E:C, $F:Y, $T:C} (before narrowing)
        // After narrowing to query vars (A, C - which are concrete), both yield an empty binding.
        // So, `distinct()` should result in 1.
        //
        // Let's re-verify `executePathStarTraversal`:
        // `finalResults.add(merged.get(0));` where `merged` is `currentPathBindings.copy().merge(endMatchSpecificBindings);`
        // `currentPathBindings` for path A-X-C when C is hit would have involved X.
        // `currentPathBindings` for path A-Y-C when C is hit would have involved Y.
        // So, even if `endMatchSpecificBindings` is empty (C matches C), the `currentPathBindings` are different.
        // And `narrowVariables` is applied *after* all results are collected.
        // So, `finalResults` will contain two different Bindings objects initially if their history differs,
        // even if they narrow to the same final set of relevant variable bindings.
        // If `Bindings.equals()` is sensitive to all bound variables (not just relevant ones), they'd be distinct.
        // But `Bindings.equals()` should ideally work on the *semantic content* of the bindings.
        // The current `Bindings.equals` (if not overridden, from Object) would treat different instances as non-equal.
        // If `Bindings` has a proper equals method based on its content, then two empty Bindings are equal.
        //
        // The prompt for the original subtask had:
        // "The current BFS with `visitedWithDepth` might only find one if both paths have same length." - This is about `visitedWithDepth` for *nodes*, not final paths.
        // "The `finalResults` list collects all bindings that satisfy the end condition. So, if C is reached via two distinct bindings sets at depth 2, both should be added. Distinct on final results will handle it."
        //
        // If the two paths produce two `Bindings` objects that are semantically identical after narrowing (e.g. empty),
        // then `distinct()` will reduce them to 1. This seems to be the correct interpretation.
        assertEquals(1, results.size(), "Expected 1 distinct binding set for A->C, even via multiple paths, as A and C are concrete.");
    }
}
