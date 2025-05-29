package com.example.metta.space;

import com.example.metta.atom.Atom;
import com.example.metta.types.Bindings;

import java.util.List;

public interface SpaceReader {
    List<Bindings> query(Atom queryPattern);
    List<Atom> subst(Atom pattern, Atom template);
    List<Atom> getAtoms(); // Potentially large, consider alternatives for huge spaces if performance becomes an issue.
    boolean contains(Atom atom);
}
