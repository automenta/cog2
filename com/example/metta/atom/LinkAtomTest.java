package com.example.metta.atom;

import com.example.metta.space.GroundingSpace;
import com.example.metta.types.TypeChecker; // Assuming this is the correct location
import com.example.metta.types.AtomType; // Added import
import com.example.metta.text.SExprParser; // For parsing helper atoms if needed

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public class LinkAtomTest {

    private Atom parse(String mettaSrc) {
        // Corrected SExprParser usage based on previous findings
        SExprParser parser = new SExprParser();
        return parser.parse(mettaSrc);
    }

    @Test
    void testLinkAtomCreationAndGetters() {
        SymbolAtom type = new SymbolAtom("MyLink");
        SymbolAtom target1 = new SymbolAtom("a");
        ExpressionAtom target2 = new ExpressionAtom(List.of(new SymbolAtom("b"), new SymbolAtom("c")));

        LinkAtom link = new LinkAtom(type, Arrays.asList(target1, target2));

        assertEquals(type, link.getLinkType());
        assertEquals(Arrays.asList(target1, target2), link.getTargets());
        assertEquals(2, link.getTargets().size());
        assertSame(target1, link.getTargets().get(0));
        assertSame(target2, link.getTargets().get(1));

        // Test with empty targets
        LinkAtom linkEmptyTargets = new LinkAtom(type, Collections.emptyList());
        assertEquals(type, linkEmptyTargets.getLinkType());
        assertTrue(linkEmptyTargets.getTargets().isEmpty());
    }

    @Test
    void testLinkAtomEqualityAndHashCode() {
        SymbolAtom type1 = new SymbolAtom("TypeA");
        SymbolAtom type2 = new SymbolAtom("TypeB");
        SymbolAtom targetA = new SymbolAtom("a");
        SymbolAtom targetB = new SymbolAtom("b");

        LinkAtom link1a = new LinkAtom(type1, Arrays.asList(targetA, targetB));
        LinkAtom link1b = new LinkAtom(type1, Arrays.asList(targetA, targetB)); // Same as link1a
        LinkAtom link2 = new LinkAtom(type1, Arrays.asList(targetB, targetA)); // Different target order
        LinkAtom link3 = new LinkAtom(type2, Arrays.asList(targetA, targetB)); // Different type
        LinkAtom link4 = new LinkAtom(type1, Arrays.asList(targetA)); // Different target count
        LinkAtom link5 = new LinkAtom(type1, Collections.emptyList());
        LinkAtom link6 = new LinkAtom(type1, Collections.emptyList());


        assertEquals(link1a, link1b);
        assertEquals(link1a.hashCode(), link1b.hashCode());

        assertEquals(link5, link6);
        assertEquals(link5.hashCode(), link6.hashCode());

        assertNotEquals(link1a, link2);
        assertNotEquals(link1a, link3);
        assertNotEquals(link1a, link4);
        assertNotEquals(link1a, targetA); // Different class
        assertNotEquals(link1a, null);
    }

    @Test
    void testLinkAtomToString() {
        SymbolAtom type = new SymbolAtom("CustomLink");
        SymbolAtom target1 = new SymbolAtom("x");
        SymbolAtom target2 = new SymbolAtom("y");
        ExpressionAtom target3 = new ExpressionAtom(List.of(new SymbolAtom("z"), new SymbolAtom("w")));

        LinkAtom link1 = new LinkAtom(type, Arrays.asList(target1, target2));
        assertEquals("CustomLink(x y)", link1.toString());

        LinkAtom link2 = new LinkAtom(type, Arrays.asList(target1, target2, target3));
        assertEquals("CustomLink(x y (z w))", link2.toString());

        LinkAtom linkEmpty = new LinkAtom(type, Collections.emptyList());
        assertEquals("CustomLink()", linkEmpty.toString());
    }

    @Test
    void testLinkAtomInGroundingSpace() {
        GroundingSpace space = new GroundingSpace();
        LinkAtom link = new LinkAtom((SymbolAtom) MettaSymbols.INHERITANCE_LINK_SYMBOL,
                                   Arrays.asList(new SymbolAtom("cat"), new SymbolAtom("mammal")));
        space.add(link);
        assertTrue(space.getAtoms().contains(link));
        assertEquals(1, space.getAtoms().size());

        // Try adding the same link again
        space.add(link);
        assertEquals(1, space.getAtoms().size(), "Adding identical link should not increase size.");
    }

    @Test
    void testLinkAtomTypeChecking() {
        GroundingSpace space = new GroundingSpace(); // TypeChecker might need a space for some operations.

        SymbolAtom cat = new SymbolAtom("cat");
        SymbolAtom mammal = new SymbolAtom("mammal");
        LinkAtom inheritance = new LinkAtom((SymbolAtom) MettaSymbols.INHERITANCE_LINK_SYMBOL, Arrays.asList(cat, mammal));

        // Test getAtomTypes
        List<AtomType> types = TypeChecker.getAtomTypes(space, inheritance);

        // Expected: InheritanceLink and Link
        assertTrue(types.stream().anyMatch(at -> at.getAtom().equals(MettaSymbols.INHERITANCE_LINK_SYMBOL)),
                   "Should be typed as InheritanceLink. Types: " + types);
        assertTrue(types.stream().anyMatch(at -> at.getAtom().equals(MettaSymbols.LINK_TYPE_SYMBOL)),
                   "Should be typed as Link. Types: " + types);

        // Test checkType
        assertTrue(TypeChecker.checkType(space, inheritance, MettaSymbols.INHERITANCE_LINK_SYMBOL),
                   "checkType for specific link type failed.");
        assertTrue(TypeChecker.checkType(space, inheritance, MettaSymbols.LINK_TYPE_SYMBOL),
                   "checkType for general Link type failed.");
        assertFalse(TypeChecker.checkType(space, inheritance, MettaSymbols.EVALUATION_LINK_SYMBOL),
                   "checkType for incorrect specific link type should fail.");
        assertTrue(TypeChecker.checkType(space, inheritance, MettaSymbols.ATOM_TYPE),
                    "checkType for Atom (general atom type) should be true for links.");
    }
}
