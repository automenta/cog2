package com.example.metta.interpreter;

import com.example.metta.atom.Atom;
import com.example.metta.space.SpaceReader;
import com.example.metta.types.Bindings;
import com.example.metta.matcher.Matcher; 

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;

public class InterpreterState {
    private final Deque<Pair<StackFrame, Bindings>> plan;
    private final List<Atom> finishedResults;
    private final InterpreterContext context;
    private int maxStackDepth = 256;

    private static final ReturnHandler TOP_LEVEL_RETURN_HANDLER = (parentFrame, resultAtom, resultBindings) -> {
        System.err.println("Warning: TOP_LEVEL_RETURN_HANDLER invoked. Parent: " + parentFrame + ", Result: " + resultAtom);
        return Optional.empty(); 
    };

    public InterpreterState(SpaceReader space, Atom initialAtom) {
        this.context = new InterpreterContext(Objects.requireNonNull(space, "InterpreterState space cannot be null"));
        this.plan = new ArrayDeque<>();
        this.finishedResults = new ArrayList<>();
        Objects.requireNonNull(initialAtom, "InterpreterState initialAtom cannot be null");

        StackFrame initialFrame = new StackFrame(null, initialAtom, TOP_LEVEL_RETURN_HANDLER, 0);
        this.plan.push(new Pair<>(initialFrame, new Bindings()));
    }

    public boolean hasNext() {
        return !plan.isEmpty();
    }

    public Pair<StackFrame, Bindings> pop() {
        if (!hasNext()) {
            throw new IllegalStateException("Cannot pop from an empty plan.");
        }
        return plan.pop();
    }

    public void push(StackFrame frame, Bindings bindings) {
        Objects.requireNonNull(frame, "StackFrame to push cannot be null");
        Objects.requireNonNull(bindings, "Bindings to push cannot be null");

        if (frame.isFinished() && frame.getPreviousFrame() == null) {
            Atom finalAtom = Matcher.applyBindings(frame.getCurrentAtom(), bindings);
            this.finishedResults.add(finalAtom);
        } else {
            this.plan.push(new Pair<>(frame, bindings));
        }
    }
    
    public void pushAll(List<Pair<StackFrame, Bindings>> states) {
        Objects.requireNonNull(states, "States to push cannot be null");
        for (int i = states.size() - 1; i >= 0; i--) {
            Pair<StackFrame, Bindings> statePair = states.get(i);
            Objects.requireNonNull(statePair, "StatePair cannot be null in pushAll");
            Objects.requireNonNull(statePair.getLeft(), "StackFrame in StatePair cannot be null in pushAll");
            Objects.requireNonNull(statePair.getRight(), "Bindings in StatePair cannot be null in pushAll");
            push(statePair.getLeft(), statePair.getRight());
        }
    }

    public List<Atom> getResults() {
        if (hasNext()) {
            return Collections.emptyList(); 
        }
        return Collections.unmodifiableList(finishedResults);
    }

    public InterpreterContext getContext() {
        return context;
    }

    public int getMaxStackDepth() {
        return maxStackDepth;
    }

    public void setMaxStackDepth(int maxStackDepth) {
        if (maxStackDepth <= 0) {
            throw new IllegalArgumentException("Max stack depth must be positive.");
        }
        this.maxStackDepth = maxStackDepth;
    }
}
