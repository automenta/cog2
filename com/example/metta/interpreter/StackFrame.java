package com.example.metta.interpreter;

import com.example.metta.atom.Atom;
import java.util.Objects;

public class StackFrame {
    private final StackFrame previousFrame;
    private Atom currentAtom; 
    private final ReturnHandler returnHandler;
    private boolean isFinished;
    private final int depth;

    public StackFrame(StackFrame previousFrame, Atom currentAtom, ReturnHandler returnHandler, int depth) {
        this.previousFrame = previousFrame;
        this.currentAtom = Objects.requireNonNull(currentAtom, "StackFrame currentAtom cannot be null");
        this.returnHandler = Objects.requireNonNull(returnHandler, "StackFrame returnHandler cannot be null");
        this.isFinished = false; 
        this.depth = depth;
    }

    public StackFrame getPreviousFrame() {
        return previousFrame;
    }

    public Atom getCurrentAtom() {
        return currentAtom;
    }

    public void setCurrentAtom(Atom currentAtom) {
        this.currentAtom = Objects.requireNonNull(currentAtom, "StackFrame currentAtom for set cannot be null");
    }

    public ReturnHandler getReturnHandler() {
        return returnHandler;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "Frame[D:" + depth + ", A:" + currentAtom + (isFinished ? ", done" : "") + "]";
    }
}
