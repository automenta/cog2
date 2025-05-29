package com.example.metta.interpreter;

import com.example.metta.space.SpaceReader;
import java.util.Objects;

public class InterpreterContext {
    private final SpaceReader space;

    public InterpreterContext(SpaceReader space) {
        this.space = Objects.requireNonNull(space, "InterpreterContext space cannot be null");
    }

    public SpaceReader getSpace() {
        return space;
    }
}
