package com.example.metta.space;

import com.example.metta.atom.Atom;

public interface SpaceWriter extends SpaceReader {
    void add(Atom atom);
    boolean remove(Atom atom);
    boolean replace(Atom from, Atom to);
}
