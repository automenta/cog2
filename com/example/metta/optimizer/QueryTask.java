package com.example.metta.optimizer;

import com.example.metta.atom.Atom;
import com.example.metta.types.Bindings;
import java.util.List;
import java.util.function.Predicate;

public class QueryTask {
    private final Atom queryAtom;
    private final String description;
    private final Predicate<List<Bindings>> successCondition;

    public QueryTask(Atom queryAtom, String description, Predicate<List<Bindings>> successCondition) {
        this.queryAtom = queryAtom;
        this.description = description;
        this.successCondition = successCondition;
    }

    public Atom getQueryAtom() {
        return queryAtom;
    }

    public String getDescription() {
        return description;
    }

    public Predicate<List<Bindings>> getSuccessCondition() {
        return successCondition;
    }
}
