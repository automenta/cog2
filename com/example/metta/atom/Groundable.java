package com.example.metta.atom;

import com.example.metta.types.Bindings;
import java.util.List;

public interface Groundable {
    Atom getType();
    List<Atom> execute(List<Atom> args); // TODO: Consider if Bindings context should be passed here
    List<Bindings> match(Atom other); // TODO: Consider if Bindings context should be passed or returned differently
    String toDisplayString();
}
