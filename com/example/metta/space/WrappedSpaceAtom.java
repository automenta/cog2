package com.example.metta.space;

import com.example.metta.atom.AbstractAtom;
import com.example.metta.atom.Atom;
import com.example.metta.atom.Groundable;
import com.example.metta.atom.MettaSymbols;
import com.example.metta.types.Bindings;

import java.util.List;
import java.util.Objects;

public class WrappedSpaceAtom extends AbstractAtom implements Groundable {

    private final SpaceWriter wrappedSpace;

    public WrappedSpaceAtom(SpaceWriter wrappedSpace) {
        this.wrappedSpace = Objects.requireNonNull(wrappedSpace, "wrappedSpace cannot be null");
    }

    public SpaceWriter getWrappedSpace() {
        return wrappedSpace;
    }

    @Override
    public Atom getType() {
        return MettaSymbols.SPACE_TYPE;
    }

    @Override
    public List<Atom> execute(List<Atom> args) {
        throw new UnsupportedOperationException("WrappedSpaceAtom is not directly executable. Query via match() or specific operations.");
    }

    @Override
    public List<Bindings> match(Atom otherPattern) {
        return this.wrappedSpace.query(otherPattern);
    }

    @Override
    public String toDisplayString() {
        return "<Space@" + Integer.toHexString(System.identityHashCode(wrappedSpace)) + ">";
    }

    @Override
    public String toString() {
        return toDisplayString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WrappedSpaceAtom)) return false;
        WrappedSpaceAtom that = (WrappedSpaceAtom) obj;
        return this.wrappedSpace == that.wrappedSpace; // Identity comparison for wrapped space
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(wrappedSpace);
    }
}
