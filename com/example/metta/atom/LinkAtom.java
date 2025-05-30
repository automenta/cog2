package com.example.metta.atom;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a Link Atom, a typed connection between a set of target atoms.
 * Links are fundamental for expressing structured relationships in Metta,
 * similar to links in OpenCog's AtomSpace.
 * The link has a specific type (a SymbolAtom) and an ordered list of targets.
 * LinkAtoms are immutable.
 */
public final class LinkAtom extends AbstractAtom {
    private final SymbolAtom linkType;
    private final List<Atom> targets;

    /**
     * Constructs a new LinkAtom.
     *
     * @param linkType The type of the link, represented by a SymbolAtom. Cannot be null.
     * @param targets  The ordered list of target atoms this link connects. Cannot be null;
     *                 the list itself and its elements also cannot be null.
     *                 A defensive copy of the list is made.
     */
    public LinkAtom(SymbolAtom linkType, List<Atom> targets) {
        this.linkType = Objects.requireNonNull(linkType, "linkType cannot be null");
        Objects.requireNonNull(targets, "targets list cannot be null");
        // Ensure no nulls in targets list and make a defensive copy
        this.targets = List.copyOf(targets.stream()
                                        .map(t -> Objects.requireNonNull(t, "target atom within list cannot be null"))
                                        .collect(Collectors.toList()));
    }

    /**
     * Gets the type of this link.
     * @return The {@link SymbolAtom} representing the link's type.
     */
    public SymbolAtom getLinkType() {
        return linkType;
    }

    /**
     * Gets the ordered list of target atoms connected by this link.
     * The returned list is unmodifiable.
     * @return An unmodifiable {@link List} of {@link Atom}s representing the targets.
     */
    public List<Atom> getTargets() {
        return targets; // Already unmodifiable from List.copyOf()
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LinkAtom)) return false;
        LinkAtom that = (LinkAtom) obj;
        return linkType.equals(that.linkType) &&
               targets.equals(that.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkType, targets);
    }

    /**
     * Returns a string representation of this LinkAtom.
     * The format is "linkTypeSymbol(target1 target2 ...)", e.g., "InheritanceLink(cat mammal)".
     * If there are no targets, it will be "linkTypeSymbol()".
     * @return The string representation.
     */
    @Override
    public String toString() {
        return linkType.getName() + targets.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" ", "(", ")"));
    }
}
