package org.opencog.hyperon.metta.interpreter;

import org.opencog.hyperon.atoms.*;
import org.opencog.hyperon.space.AtomSpace;
import org.opencog.hyperon.space.Bindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MettaEngine {
    private final AtomSpace space;
    private final GroundedAtomRegistry groundedAtomRegistry;

    public MettaEngine(AtomSpace space, GroundedAtomRegistry registry) {
        this.space = space;
        this.groundedAtomRegistry = registry;
    }

    public List<Atom> evaluate(Atom expression, Bindings initialBindings) {
        if (expression instanceof SymbolAtom) {
            return Collections.singletonList(expression);
        } else if (expression instanceof VariableAtom) {
            VariableAtom var = (VariableAtom) expression;
            if (initialBindings.isBound(var)) {
                return evaluate(initialBindings.getValue(var), initialBindings);
            } else {
                return Collections.singletonList(expression); // Unbound variable evaluates to itself
            }
        } else if (expression instanceof GroundedAtom) {
            // Direct evaluation of a GroundedAtom (not as head of expression) returns itself.
            // Its execution happens when it's the head of an expression.
            return Collections.singletonList(expression);
        } else if (expression instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) expression;
            List<Atom> results = new ArrayList<>();

            if (expr.getChildren().isEmpty()) {
                // Empty expression evaluates to itself or a special EmptySymbol if desired
                return Collections.singletonList(expr);
            }

            Atom head = expr.getChildren().get(0);
            List<Atom> args = expr.getChildren().subList(1, expr.getChildren().size());

            Groundable operationToExecute = null;
            String operationName = null;

            if (head instanceof GroundedAtom) {
                operationToExecute = ((GroundedAtom) head).getOperation();
                operationName = ((GroundedAtom) head).getName();
            } else if (head instanceof SymbolAtom && groundedAtomRegistry.isRegistered(((SymbolAtom) head).getName())) {
                operationName = ((SymbolAtom) head).getName();
                operationToExecute = groundedAtomRegistry.getOperation(operationName);
            }

            if (operationToExecute != null) {
                List<Atom> evaluatedArgs = new ArrayList<>();
                boolean argEvalFailed = false;
                for (Atom arg : args) {
                    List<Atom> evaledArgResults = evaluate(arg, initialBindings);
                    if (evaledArgResults.isEmpty()) {
                        argEvalFailed = true;
                        break;
                    }
                    evaluatedArgs.add(evaledArgResults.get(0));
                }

                if (!argEvalFailed) {
                    try {
                        Atom groundedResult = operationToExecute.execute(evaluatedArgs);
                        results.addAll(evaluate(groundedResult, initialBindings));
                    } catch (Exception e) {
                        System.err.println("Error executing operation " + (operationName != null ? operationName : "anonymous_grounded_atom") + ": " + e.getMessage());
                    }
                }
            } else {
                // No direct grounded operation found for the head, try rule matching.
                // Using a unique variable name for $Res to avoid clashes.
                VariableAtom resultVar = new VariableAtom("$Res_MettaEngine_" + System.nanoTime());
                Atom queryPattern = new ExpressionAtom(List.of(new SymbolAtom("="), expr, resultVar));

                List<Bindings> ruleMatches = space.query(queryPattern);

                for (Bindings matchBinding : ruleMatches) {
                    Atom ruleBody = matchBinding.getValue(resultVar);
                    if (ruleBody != null) {
                        Atom partiallySubstitutedBody = matchBinding.substitute(ruleBody);
                        results.addAll(evaluate(partiallySubstitutedBody, initialBindings));
                    }
                }
            }

            // 3. Self-Evaluation (if no results from grounded op or rules)
            if (results.isEmpty() && operationToExecute == null) {
                // If operationToExecute was not null, it means an attempt to execute it was made.
                // If results are still empty, it means the op execution itself yielded nothing (or failed silently).
                // In that case, we don't evaluate to self.
                // If operationToExecute was null, it means no direct op was found, and then if rules also yielded nothing, evaluate to self.
                results.add(expr);
            }
            return results.stream().distinct().collect(Collectors.toList());
        }

        return Collections.emptyList(); // Should not be reached for valid Atom types
    }
}
