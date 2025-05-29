package com.example.metta.interpreter;

import com.example.metta.atom.Atom;
import com.example.metta.types.Bindings;
import java.util.Optional;

@FunctionalInterface
public interface ReturnHandler {
    Optional<Pair<StackFrame, Bindings>> apply(StackFrame currentParentFrame, Atom resultAtom, Bindings resultBindings);
}
