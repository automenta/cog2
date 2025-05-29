package com.example.metta.interpreter;

import com.example.metta.atom.*;
import com.example.metta.matcher.Matcher;
import com.example.metta.space.SpaceReader;
import com.example.metta.types.Bindings;
import com.example.metta.types.TypeChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects; // For requireNonNull

public class Interpreter {

    private Interpreter() {} // Private constructor for utility class

    private static final ReturnHandler PASS_TO_PARENT_HANDLER = (parentFrame, resultAtom, resultBindings) -> {
        if (parentFrame == null) {
            System.err.println("Error: PASS_TO_PARENT_HANDLER called with null parentFrame for result: " + resultAtom);
            return Optional.empty(); 
        }
        parentFrame.setCurrentAtom(resultAtom);
        parentFrame.setFinished(false); 
        return Optional.of(new Pair<>(parentFrame, resultBindings));
    };
    
    private static final ReturnHandler FINISH_CURRENT_FRAME_HANDLER = (currentFrameHost, resultAtom, resultBindings) -> {
        if (currentFrameHost == null) {
             System.err.println("Error: FINISH_CURRENT_FRAME_HANDLER called with null currentFrameHost for result: " + resultAtom);
            return Optional.empty();
        }
        currentFrameHost.setCurrentAtom(resultAtom);
        currentFrameHost.setFinished(true); 
        return Optional.of(new Pair<>(currentFrameHost, resultBindings));
    };

    private static final ReturnHandler TOP_LEVEL_FRAME_HANDLER_SENTINEL = (parent, resultAtom, bindings) -> {
        System.err.println("Warning: TOP_LEVEL_FRAME_HANDLER_SENTINEL was unexpectedly invoked for frame: " + parent + " with result: " + resultAtom);
        return Optional.empty();
    };

    public static InterpreterState interpret_step(InterpreterState state) {
        if (!state.hasNext()) {
            return state;
        }

        Pair<StackFrame, Bindings> currentPlanItem = state.pop();
        StackFrame frame = currentPlanItem.getLeft();
        Bindings bindings = currentPlanItem.getRight();

        if (frame.isFinished()) {
            StackFrame parentFrame = frame.getPreviousFrame();
            if (parentFrame != null) { // If null, it's a top-level frame, already handled by InterpreterState.push
                Optional<Pair<StackFrame, Bindings>> nextStateOpt = parentFrame.getReturnHandler()
                        .apply(parentFrame, frame.getCurrentAtom(), bindings);
                nextStateOpt.ifPresent(next -> state.push(next.getLeft(), next.getRight()));
            }
        } else {
            Atom atom = frame.getCurrentAtom();
            Atom concreteAtom = Matcher.applyBindings(atom, bindings);
            // Update currentAtom in frame *only if* it changed, to preserve original structure if no vars were substituted.
            // This is important if the original atom structure (e.g. with specific variable instances) is needed by a handler.
            // However, handlers generally should operate on the concreteAtom. For now, always update.
            frame.setCurrentAtom(concreteAtom); 

            List<Pair<StackFrame, Bindings>> nextSteps = processAtom(state.getContext(), frame, bindings, concreteAtom, state.getMaxStackDepth());
            
            if (!nextSteps.isEmpty()) {
                state.pushAll(nextSteps);
            } else {
                // If no next steps from handlers, the atom evaluates to itself (it's a value or irreducible)
                // or it was an operation that directly modified the frame (e.g. by a special return handler).
                // If the frame wasn't marked finished by a handler, mark it now.
                if (!frame.isFinished()) {
                    frame.setFinished(true);
                    state.push(frame, bindings); // Push it back to be processed by its parent's return handler
                }
            }
        }
        return state;
    }

    private static List<Pair<StackFrame, Bindings>> processAtom(
            InterpreterContext context, StackFrame frame, Bindings bindings, Atom concreteAtom, int maxDepth) {

        if (concreteAtom instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) concreteAtom;
            if (expr.getChildren().isEmpty()) { // Empty expression ()
                frame.setCurrentAtom(MettaSymbols.UNIT_TYPE); 
                frame.setFinished(true);
                return Collections.singletonList(new Pair<>(frame, bindings));
            }

            Atom operator = expr.getChildren().get(0);
            if (operator.equals(MettaSymbols.EVAL_SYMBOL) || operator.equals(MettaSymbols.METTA_SYMBOL)) {
                if (frame.getDepth() > maxDepth) {
                    Atom errorAtom = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("StackOverflow"), concreteAtom));
                    frame.setCurrentAtom(errorAtom);
                    frame.setFinished(true);
                    return Collections.singletonList(new Pair<>(frame, bindings));
                }
            }

            if (operator.equals(MettaSymbols.EVAL_SYMBOL)) {
                return handleEval(context, frame, bindings, false);
            } else if (operator.equals(MettaSymbols.CHAIN_SYMBOL)) {
                return handleChain(context, frame, bindings);
            } else if (operator.equals(MettaSymbols.UNIFY_SYMBOL)) {
                 return handleUnify(context, frame, bindings);
            } else if (operator.equals(MettaSymbols.DECONS_SYMBOL)) {
                return handleDecons(frame, bindings);
            } else if (operator.equals(MettaSymbols.CONS_SYMBOL)) {
                return handleCons(frame, bindings);
            } else if (operator.equals(MettaSymbols.FUNCTION_SYMBOL)) {
                frame.setFinished(true); // (function <Body>) evaluates to itself (the body expression for later execution)
                return Collections.singletonList(new Pair<>(frame, bindings));
            } else if (operator.equals(MettaSymbols.METTA_SYMBOL)){
                return handleMetta(context, frame, bindings);
            } else if (operator.equals(MettaSymbols.COLLAPSE_BIND_SYMBOL) || operator.equals(MettaSymbols.SUPERPOSE_BIND_SYMBOL)) {
                 Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("UnsupportedOperation"), operator));
                 frame.setCurrentAtom(error);
                 frame.setFinished(true);
                 return Collections.singletonList(new Pair<>(frame, bindings));
            } else { 
                return handleEval(context, frame, bindings, true); // Implicit eval for other expressions
            }
        } else { 
            return handleEval(context, frame, bindings, true); // Implicit eval for Symbols, Variables, GroundedAtoms
        }
    }
    
    private static List<Pair<StackFrame, Bindings>> handleEval(InterpreterContext context, StackFrame currentFrame, Bindings currentBindings, boolean isImplicit) {
        Atom atomToEvaluate = currentFrame.getCurrentAtom(); 
        
        if (!isImplicit && atomToEvaluate instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) atomToEvaluate;
            // If it's (eval <actual_atom>), then actual_atom_to_eval is children.get(1)
            if (!expr.getChildren().isEmpty() && expr.getChildren().get(0).equals(MettaSymbols.EVAL_SYMBOL)) {
                if (expr.getChildren().size() > 1) {
                    atomToEvaluate = expr.getChildren().get(1);
                    // Update currentFrame's atom to be the actual atom being evaluated, for clarity in handlers
                    currentFrame.setCurrentAtom(atomToEvaluate);
                } else { 
                    Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("MissingArgument"), MettaSymbols.EVAL_SYMBOL));
                    currentFrame.setCurrentAtom(error);
                    currentFrame.setFinished(true);
                    return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
                }
            }
        }
        
        List<Pair<StackFrame, Bindings>> nextSteps = new ArrayList<>();

        if (atomToEvaluate instanceof ExpressionAtom) {
            ExpressionAtom exprToExecute = (ExpressionAtom) atomToEvaluate;
            if (!exprToExecute.getChildren().isEmpty() && exprToExecute.getChildren().get(0) instanceof GroundedAtom) {
                GroundedAtom opGrounded = (GroundedAtom) exprToExecute.getChildren().get(0);
                if (opGrounded.getGroundableInterface() instanceof Executable) {
                    return evaluateExecutableGroundedAtom(context, currentFrame, currentBindings, opGrounded, exprToExecute.getChildren().subList(1, exprToExecute.getChildren().size()));
                }
            }
        } else if (atomToEvaluate instanceof GroundedAtom && ((GroundedAtom) atomToEvaluate).getGroundableInterface() instanceof Executable){
             return evaluateExecutableGroundedAtom(context, currentFrame, currentBindings, (GroundedAtom)atomToEvaluate, Collections.emptyList());
        }

        VariableAtom resultVar = new VariableAtom("$Res_eval_" + currentFrame.getDepth()); // Make var unique per frame
        ExpressionAtom query = new ExpressionAtom(List.of(MettaSymbols.EQUAL_SYMBOL, atomToEvaluate, resultVar));
        List<Bindings> spaceQueryResults = context.getSpace().query(query);

        if (!spaceQueryResults.isEmpty()) {
            for (Bindings b : spaceQueryResults) {
                Atom resolvedResult = b.resolve(resultVar);
                if (resolvedResult != null) {
                    ReturnHandler parentHandler = currentFrame.getPreviousFrame() == null ? TOP_LEVEL_FRAME_HANDLER_SENTINEL : currentFrame.getReturnHandler();
                    if (currentFrame.getPreviousFrame() != null) parentHandler = currentFrame.getPreviousFrame().getReturnHandler();


                    StackFrame resultProcessingFrame = new StackFrame(
                        currentFrame.getPreviousFrame(), 
                        resolvedResult,
                        parentHandler, 
                        currentFrame.getDepth() // Result processed at same logical depth as original query to space
                    );
                    Bindings newBindings = currentBindings.copy();
                    newBindings.merge(b); 
                    nextSteps.add(new Pair<>(resultProcessingFrame, newBindings));
                }
            }
            // The original frame for `atomToEvaluate` is finished because its evaluation path is via space query.
            // Its results are pushed as new frames. If there were no valid results from query, it would fall through.
            // However, if spaceQueryResults is not empty but all results are null (shouldn't happen with query structure),
            // then it might fall through. To be safe, if we found results, this path is done.
            // currentFrame.setFinished(true); // This is implicitly handled as we return nextSteps and don't re-push currentFrame.
        } else {
            currentFrame.setCurrentAtom(atomToEvaluate); 
            currentFrame.setFinished(true);
            nextSteps.add(new Pair<>(currentFrame, currentBindings));
        }
        return nextSteps;
    }
    
    private static List<Pair<StackFrame, Bindings>> handleEval(InterpreterContext context, StackFrame currentFrame, Bindings currentBindings) {
        return handleEval(context, currentFrame, currentBindings, false);
    }

    private static List<Pair<StackFrame, Bindings>> evaluateExecutableGroundedAtom(
            InterpreterContext context, StackFrame currentFrame, Bindings currentBindings,
            GroundedAtom operator, List<Atom> args) {

        Executable executable = (Executable) operator.getGroundableInterface();
        if (args.isEmpty()) {
            List<Atom> results = executable.execute(Collections.emptyList(), currentBindings); // Pass current bindings
            return processExecutableResults(results, currentFrame, currentBindings);
        }

        // Setup for argument evaluation
        ArgumentCollectorReturnHandler argCollector = new ArgumentCollectorReturnHandler(
            executable, operator, args.size(), currentFrame, currentBindings
        );
        
        // Push evaluation frames for arguments in reverse order
        List<Pair<StackFrame, Bindings>> argEvalSteps = new ArrayList<>();
        for (int i = args.size() - 1; i >= 0; i--) {
            // The parent of an arg-eval-frame is the frame that initiated the execute call (currentFrame).
            // When an arg is evaluated, it calls the argCollector.
            StackFrame argEvalFrame = new StackFrame(currentFrame, args.get(i), argCollector, currentFrame.getDepth() + 1);
            argEvalSteps.add(new Pair<>(argEvalFrame, currentBindings.copy())); 
        }
        return argEvalSteps;
    }

    private static List<Pair<StackFrame, Bindings>> processExecutableResults(List<Atom> results, StackFrame currentFrame, Bindings currentBindings) {
        List<Pair<StackFrame, Bindings>> nextSteps = new ArrayList<>();
        ReturnHandler parentHandler = currentFrame.getPreviousFrame() == null ? TOP_LEVEL_FRAME_HANDLER_SENTINEL : currentFrame.getPreviousFrame().getReturnHandler();
        
        if (results.isEmpty()) {
            currentFrame.setCurrentAtom(MettaSymbols.UNIT_TYPE);
            currentFrame.setFinished(true); // currentFrame is finished with UNIT
            // This finished currentFrame needs to be passed to its parent's handler.
            // So, the "next step" is the invocation of currentFrame's parent's handler.
            // This is achieved by pushing currentFrame (marked as finished) back.
            nextSteps.add(new Pair<>(currentFrame, currentBindings));

        } else {
            for (Atom res : results) {
                // Each result forms a new path of execution, replacing the currentFrame's path,
                // but returning to currentFrame's parent.
                StackFrame resultFrame = new StackFrame(
                    currentFrame.getPreviousFrame(), 
                    res,
                    parentHandler,
                    currentFrame.getDepth() // Results are at the same conceptual depth of the original call
                );
                nextSteps.add(new Pair<>(resultFrame, currentBindings.copy())); // Each result path gets a copy of bindings
            }
        }
        return nextSteps;
    }


    private static class ArgumentCollectorReturnHandler implements ReturnHandler {
        private final Executable executableOp;
        private final GroundedAtom operatorAtom; // For context/debugging
        private final int totalArgsExpected;
        private final List<Atom> collectedArgs; // Should be one list per instance
        private final StackFrame mainOpFrame; // The frame of (op arg1 arg2...)
        private final Bindings initialBindings; // Bindings at the start of the function call

        public ArgumentCollectorReturnHandler(Executable executableOp, GroundedAtom operatorAtom, 
                                            int totalArgsExpected, StackFrame mainOpFrame, Bindings initialBindings) {
            this.executableOp = executableOp;
            this.operatorAtom = operatorAtom;
            this.totalArgsExpected = totalArgsExpected;
            this.collectedArgs = new ArrayList<>(totalArgsExpected); // Initialize with capacity
            this.mainOpFrame = mainOpFrame;
            this.initialBindings = initialBindings.copy(); // Copy to isolate bindings for this call
        }

        @Override
        public Optional<Pair<StackFrame, Bindings>> apply(StackFrame argFrameHost, Atom evaluatedArg, Bindings argEvalBindings) {
            // argFrameHost is mainOpFrame.
            // The evaluatedArg is one of the arguments.
            // argEvalBindings are the bindings *after* evaluating that single argument.
            // We need to merge these bindings into our collected initialBindings for the call.
            // This sequential merge might be too simple if args have complex interdependencies
            // or if an arg eval fails and should stop further processing.
            this.initialBindings.merge(argEvalBindings); // Accumulate bindings from arg evaluations

            collectedArgs.add(evaluatedArg);

            if (collectedArgs.size() == totalArgsExpected) {
                // All arguments collected, execute the operation.
                List<Atom> finalArgs = Collections.unmodifiableList(new ArrayList<>(collectedArgs)); // Make a copy for safety
                collectedArgs.clear(); // Reset for potential re-use if handler structure was different (not an issue here)
                
                List<Atom> executionResults = executableOp.execute(finalArgs, this.initialBindings); // Pass accumulated bindings
                
                // The results of executableOp.execute are the results for mainOpFrame.
                // We need to create new plan items for each result, to be handled by mainOpFrame's parent.
                // This ReturnHandler itself shouldn't push to plan. It should prepare mainOpFrame.
                // This is where the previous model of returning a list of frames from a handler would be useful.
                // For now, let's assume the first result is taken, or special handling for list of results.
                // This is a critical point for multi-valued return from grounded ops.
                
                // If we adapt the main loop to take List<Pair<StackFrame, Bindings>> from ReturnHandler:
                // List<Pair<StackFrame, Bindings>> resultFrames = new ArrayList<>();
                // ... populate resultFrames ...
                // return resultFrames; (This would require changing ReturnHandler's signature)

                // Sticking to current ReturnHandler signature:
                // We make the mainOpFrame finished, with its atom being the (first) result.
                Atom resultAtom;
                if (executionResults.isEmpty()) {
                    resultAtom = MettaSymbols.UNIT_TYPE;
                } else {
                    if (executionResults.size() > 1) {
                        System.err.println("Warning: Executable " + operatorAtom + " returned multiple results. Taking first.");
                    }
                    resultAtom = executionResults.get(0);
                }
                mainOpFrame.setCurrentAtom(resultAtom);
                mainOpFrame.setFinished(true);
                return Optional.of(new Pair<>(mainOpFrame, this.initialBindings)); // Use accumulated bindings
            } else {
                // Not all arguments collected yet. The mainOpFrame remains waiting.
                // The next argument evaluation is already on the plan.
                return Optional.empty(); 
            }
        }
    }
    
    private static class ChainReturnHandler implements ReturnHandler {
        private final VariableAtom varToBind;
        private final Atom template;
        private final StackFrame originalChainFrame; 

        public ChainReturnHandler(VariableAtom varToBind, Atom template, StackFrame originalChainFrame) {
            this.varToBind = varToBind;
            this.template = template;
            this.originalChainFrame = originalChainFrame;
        }

        @Override
        public Optional<Pair<StackFrame, Bindings>> apply(StackFrame frameThatHostedNestedEval, Atom nestedResult, Bindings nestedBindings) {
            // frameThatHostedNestedEval should be originalChainFrame
            Bindings newBindings = nestedBindings.copy();
            if (!newBindings.addValueBinding(varToBind, nestedResult)) {
                // Binding conflict, chain fails, result is an error for the original chain expression
                Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("ChainBindingConflict"), varToBind, nestedResult));
                originalChainFrame.setCurrentAtom(error);
                originalChainFrame.setFinished(true);
                return Optional.of(new Pair<>(originalChainFrame, nestedBindings)); // Use nestedBindings as it's the most current
            }
            
            Atom finalTemplate = Matcher.applyBindings(template, newBindings);

            StackFrame templateEvalFrame = new StackFrame(
                originalChainFrame.getPreviousFrame(), 
                finalTemplate,
                originalChainFrame.getPreviousFrame() == null ? TOP_LEVEL_FRAME_HANDLER_SENTINEL : originalChainFrame.getPreviousFrame().getReturnHandler(),
                originalChainFrame.getDepth() // Depth of the chain operation itself, not incremented for template
            );
            return Optional.of(new Pair<>(templateEvalFrame, newBindings));
        }
    }

    private static List<Pair<StackFrame, Bindings>> handleChain(InterpreterContext context, StackFrame currentFrame, Bindings currentBindings) {
        ExpressionAtom expr = (ExpressionAtom) currentFrame.getCurrentAtom(); 
        if (expr.getChildren().size() != 4) { // (chain <nested> $var <template>)
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("IncorrectArgumentsChain"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        Atom nestedAtom = expr.getChildren().get(1);
        Atom varAtom = expr.getChildren().get(2);
        Atom templateAtom = expr.getChildren().get(3);

        if (!(varAtom instanceof VariableAtom)) {
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("ExpectedVariableChain"), varAtom));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        // The currentFrame (for 'chain') becomes the "parent" that ChainReturnHandler will update or use.
        StackFrame nestedEvalFrame = new StackFrame(
            currentFrame, 
            nestedAtom,
            new ChainReturnHandler((VariableAtom) varAtom, templateAtom, currentFrame),
            currentFrame.getDepth() + 1 // Nested evaluation is one level deeper
        );
        
        // The chain frame itself isn't finished; its ReturnHandler (via nestedEvalFrame) will produce the actual result.
        // By not marking currentFrame as finished and not adding it to nextSteps, it will be discarded from plan.
        // Its continuation is solely through the ChainReturnHandler.
        return Collections.singletonList(new Pair<>(nestedEvalFrame, currentBindings));
    }


    private static List<Pair<StackFrame, Bindings>> handleUnify(InterpreterContext context, StackFrame currentFrame, Bindings currentBindings) {
        ExpressionAtom expr = (ExpressionAtom) currentFrame.getCurrentAtom();
        if (expr.getChildren().size() != 5) { // (unify <atom1> <atom2> <then> <else>)
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("IncorrectArgumentsUnify"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        Atom atom1 = expr.getChildren().get(1);
        Atom atom2 = expr.getChildren().get(2);
        Atom thenBranch = expr.getChildren().get(3);
        Atom elseBranch = expr.getChildren().get(4);

        Atom concreteAtom1 = Matcher.applyBindings(atom1, currentBindings);
        Atom concreteAtom2 = Matcher.applyBindings(atom2, currentBindings);

        List<Bindings> matchResults = Matcher.matchAtoms(concreteAtom1, concreteAtom2);

        List<Pair<StackFrame, Bindings>> nextSteps = new ArrayList<>();
        StackFrame originalParent = currentFrame.getPreviousFrame();
        ReturnHandler parentHandler = (originalParent == null) ? TOP_LEVEL_FRAME_HANDLER_SENTINEL : originalParent.getReturnHandler();

        if (!matchResults.isEmpty()) {
            for (Bindings matchBinding : matchResults) { // Potentially multiple ways to unify
                Bindings newBindings = currentBindings.copy();
                newBindings.merge(matchBinding); 
                
                Atom concreteThenBranch = Matcher.applyBindings(thenBranch, newBindings);
                StackFrame thenFrame = new StackFrame(originalParent, concreteThenBranch, parentHandler, currentFrame.getDepth() + 1);
                nextSteps.add(new Pair<>(thenFrame, newBindings));
            }
        } else {
            Atom concreteElseBranch = Matcher.applyBindings(elseBranch, currentBindings); 
            StackFrame elseFrame = new StackFrame(originalParent, concreteElseBranch, parentHandler, currentFrame.getDepth() + 1);
            nextSteps.add(new Pair<>(elseFrame, currentBindings)); // Else branch uses original bindings
        }
        return nextSteps; // The (unify ...) frame is consumed.
    }

    private static List<Pair<StackFrame, Bindings>> handleDecons(StackFrame currentFrame, Bindings currentBindings) {
        ExpressionAtom expr = (ExpressionAtom) currentFrame.getCurrentAtom();
        if (expr.getChildren().size() < 2) { 
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("IncorrectArgumentsDecons"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        Atom targetAtom = Matcher.applyBindings(expr.getChildren().get(1), currentBindings);
        if (!(targetAtom instanceof ExpressionAtom)) {
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("DeconsTargetNotExpression"), targetAtom));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        ExpressionAtom targetExpr = (ExpressionAtom) targetAtom;
        List<Atom> targetChildren = targetExpr.getChildren();
        List<Atom> patternVars = expr.getChildren().subList(2, expr.getChildren().size());

        if (targetChildren.size() != patternVars.size()) {
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("DeconsArityMismatch"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }

        Bindings newBindings = currentBindings.copy();
        boolean success = true;
        for (int i = 0; i < patternVars.size(); i++) {
            Atom varOrPattern = patternVars.get(i);
            // Apply bindings to varOrPattern itself before trying to match or bind
            Atom concreteVarOrPattern = Matcher.applyBindings(varOrPattern, newBindings);

            if (concreteVarOrPattern instanceof VariableAtom) {
                if (!newBindings.addValueBinding((VariableAtom) concreteVarOrPattern, targetChildren.get(i))) {
                    success = false; break; 
                }
            } else { 
                if (!concreteVarOrPattern.equals(targetChildren.get(i))) {
                     success = false; break;
                }
            }
        }

        if (success) {
            currentFrame.setCurrentAtom(targetExpr); 
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, newBindings));
        } else {
            Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("DeconsBindingFailed"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings)); 
        }
    }

    private static List<Pair<StackFrame, Bindings>> handleCons(StackFrame currentFrame, Bindings currentBindings) {
        ExpressionAtom expr = (ExpressionAtom) currentFrame.getCurrentAtom(); 
        List<Atom> childrenToCons = expr.getChildren().subList(1, expr.getChildren().size());
        
        List<Atom> newChildren = childrenToCons.stream()
            .map(child -> Matcher.applyBindings(child, currentBindings))
            .collect(Collectors.toList());
        
        currentFrame.setCurrentAtom(new ExpressionAtom(newChildren));
        currentFrame.setFinished(true);
        return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
    }

    private static List<Pair<StackFrame, Bindings>> handleMetta(InterpreterContext context, StackFrame currentFrame, Bindings currentBindings) {
        ExpressionAtom expr = (ExpressionAtom) currentFrame.getCurrentAtom();
        if (expr.getChildren().size() != 4) { // (metta <atom> <type> <space_atom>)
             Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("IncorrectArgumentsMetta"), expr));
            currentFrame.setCurrentAtom(error);
            currentFrame.setFinished(true);
            return Collections.singletonList(new Pair<>(currentFrame, currentBindings));
        }
        Atom atomToEval = expr.getChildren().get(1);
        Atom typeAtom = Matcher.applyBindings(expr.getChildren().get(2), currentBindings); // Type can have vars
        // Atom spaceAtom = expr.getChildren().get(3); // TODO: Use this space for type checking if needed

        ReturnHandler mettaTypeCheckHandler = (frameAfterEval, evalResult, evalBindings) -> {
            // frameAfterEval is 'currentFrame' (the 'metta' frame)
            boolean typeCheckOk = TypeChecker.checkType(context.getSpace(), evalResult, typeAtom); // Use original context space
            if (typeCheckOk) {
                frameAfterEval.setCurrentAtom(evalResult);
            } else {
                Atom error = new ExpressionAtom(List.of(MettaSymbols.ERROR_SYMBOL, new SymbolAtom("TypeMismatch"), evalResult, typeAtom));
                frameAfterEval.setCurrentAtom(error);
            }
            frameAfterEval.setFinished(true);
            return Optional.of(new Pair<>(frameAfterEval, evalBindings));
        };
        
        StackFrame evalSubFrame = new StackFrame(currentFrame, atomToEval, mettaTypeCheckHandler, currentFrame.getDepth() + 1);
        return Collections.singletonList(new Pair<>(evalSubFrame, currentBindings));
    }

    public static List<Atom> interpret(SpaceReader space, Atom expression) {
        Objects.requireNonNull(space, "Space cannot be null for interpret");
        Objects.requireNonNull(expression, "Expression to interpret cannot be null");
        InterpreterState state = new InterpreterState(space, expression);
        while (state.hasNext()) {
            state = interpret_step(state);
        }
        return state.getResults();
    }
}
