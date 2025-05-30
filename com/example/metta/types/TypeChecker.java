package com.example.metta.types;

import com.example.metta.atom.*;
import com.example.metta.matcher.Matcher;
import com.example.metta.space.SpaceReader;
import com.example.metta.space.WrappedSpaceAtom; // Added import

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.Objects; // Added for Objects.requireNonNull
import java.util.Map;
import java.util.HashMap;

public class TypeChecker {

    public static boolean isFunctionType(Atom typeAtom) {
        if (typeAtom instanceof ExpressionAtom) {
            ExpressionAtom expr = (ExpressionAtom) typeAtom;
            return !expr.getChildren().isEmpty() && expr.getChildren().get(0).equals(MettaSymbols.ARROW_SYMBOL);
        }
        return false;
    }

    public static List<Atom> getFunctionArgTypes(Atom functionTypeAtom) {
        if (!isFunctionType(functionTypeAtom)) {
            return Collections.emptyList();
        }
        ExpressionAtom expr = (ExpressionAtom) functionTypeAtom;
        List<Atom> children = expr.getChildren();
        if (children.size() < 2) { 
            return Collections.emptyList();
        }
        return (children.size() == 2) ? Collections.emptyList() : new ArrayList<>(children.subList(1, children.size() - 1));
    }

    public static Atom getFunctionReturnType(Atom functionTypeAtom) {
        if (!isFunctionType(functionTypeAtom)) {
            return MettaSymbols.UNDEF_TYPE;
        }
        ExpressionAtom expr = (ExpressionAtom) functionTypeAtom;
        List<Atom> children = expr.getChildren();
        if (children.size() < 2) { 
             return MettaSymbols.UNDEF_TYPE;
        }
        return children.get(children.size() - 1);
    }

    private static Atom makeVariablesUnique(Atom typeAtom, Map<VariableAtom, VariableAtom> mapping) {
        if (typeAtom instanceof VariableAtom) {
            return mapping.computeIfAbsent((VariableAtom) typeAtom,
                v -> new VariableAtom(v.getName() + "_" + UUID.randomUUID().toString().substring(0, 4)));
        } else if (typeAtom instanceof ExpressionAtom) {
            List<Atom> children = ((ExpressionAtom) typeAtom).getChildren();
            List<Atom> newChildren = new ArrayList<>(children.size());
            boolean changed = false;
            for (Atom child : children) {
                Atom newChild = makeVariablesUnique(child, mapping);
                if (newChild != child) {
                    changed = true;
                }
                newChildren.add(newChild);
            }
            return changed ? new ExpressionAtom(newChildren) : typeAtom;
        }
        return typeAtom;
    }

    public static List<AtomType> getAtomTypes(SpaceReader space, Atom atom) {
        List<AtomType> types = new ArrayList<>();

        if (atom instanceof SymbolAtom) {
            ExpressionAtom queryPattern = new ExpressionAtom(List.of(
                MettaSymbols.COLON_SYMBOL,
                atom,
                new VariableAtom("$type")
            ));
            List<Bindings> results = space.query(queryPattern);
            for (Bindings b : results) {
                Atom type = b.resolve(new VariableAtom("$type"));
                if (type != null) {
                    types.add(new AtomType(type, false, false));
                }
            }
        } else if (atom instanceof VariableAtom) {
            types.add(new AtomType(MettaSymbols.UNDEF_TYPE, false, false));
        } else if (atom instanceof GroundedAtom) {
            GroundedAtom gAtom = (GroundedAtom) atom;
            Atom typeFromGroundable = gAtom.getType(); 
            Atom uniqueType = makeVariablesUnique(typeFromGroundable, new HashMap<>()); // Pass new empty map for each top-level call
            types.add(new AtomType(uniqueType, false, false));
        } else if (atom instanceof ExpressionAtom) {
            ExpressionAtom exprAtom = (ExpressionAtom) atom;
            if (exprAtom.getChildren().isEmpty()) {
                types.add(new AtomType(MettaSymbols.UNIT_TYPE, false, false));
            } else {
                Atom operator = exprAtom.getChildren().get(0);
                List<Atom> arguments = exprAtom.getChildren().subList(1, exprAtom.getChildren().size());
                List<AtomType> operatorTypes = getAtomTypes(space, operator);

                for (AtomType opType : operatorTypes) {
                    if (opType.isFunction()) {
                        Atom funcTypeAtom = opType.getAtom();
                        List<Atom> formalArgTypes = getFunctionArgTypes(funcTypeAtom);
                        Atom formalReturnType = getFunctionReturnType(funcTypeAtom);

                        if (arguments.size() != formalArgTypes.size()) {
                            types.add(new AtomType(MettaSymbols.ERROR_SYMBOL, true, true)); 
                            continue;
                        }

                        List<List<AtomType>> actualArgTypesList = arguments.stream()
                            .map(arg -> getAtomTypes(space, arg))
                            .collect(Collectors.toList());
                        
                        List<Bindings> argMatchBindings = checkArgumentTypes(actualArgTypesList, formalArgTypes, new Bindings());

                        if (argMatchBindings.isEmpty()) {
                             types.add(new AtomType(MettaSymbols.ERROR_SYMBOL, true, true));
                        } else {
                            for (Bindings b : argMatchBindings) {
                                Atom boundReturnType = Matcher.applyBindings(formalReturnType, b);
                                types.add(new AtomType(boundReturnType, true, false));
                            }
                        }
                    }
                }
            }
        }

        if (types.isEmpty()) {
            types.add(new AtomType(MettaSymbols.UNDEF_TYPE, false, false));
        }
        return types.stream().distinct().collect(Collectors.toList());
    }
    
    private static List<Bindings> checkArgumentTypes(List<List<AtomType>> actualArgTypesList, List<Atom> formalArgTypes, Bindings initialBindings) {
        if (actualArgTypesList.size() != formalArgTypes.size()) {
            return Collections.emptyList();
        }

        List<Bindings> accumulatedBindings = new ArrayList<>();
        accumulatedBindings.add(initialBindings);

        for (int i = 0; i < formalArgTypes.size(); i++) {
            Atom formalType = formalArgTypes.get(i);
            List<AtomType> actualTypesForArg_i = actualArgTypesList.get(i);

            if (actualTypesForArg_i.isEmpty() || 
                (actualTypesForArg_i.size() == 1 && actualTypesForArg_i.get(0).getAtom().equals(MettaSymbols.UNDEF_TYPE))) {
                 boolean formalCanBeUndef = formalType.equals(MettaSymbols.UNDEF_TYPE) || formalType instanceof VariableAtom;
                 if(!formalCanBeUndef){
                     return Collections.emptyList(); 
                 }
            }

            List<Bindings> nextAccumulatedBindings = new ArrayList<>();
            for (Bindings currentAccBinding : accumulatedBindings) {
                Atom concreteFormalType = Matcher.applyBindings(formalType, currentAccBinding); 
                for (AtomType actualType : actualTypesForArg_i) {
                    if (actualType.isError()) continue; 

                    List<Bindings> matchResult = matchTypes(concreteFormalType, actualType.getAtom(), currentAccBinding);
                    nextAccumulatedBindings.addAll(matchResult);
                }
            }
            accumulatedBindings = nextAccumulatedBindings.stream().distinct().collect(Collectors.toList());
            if (accumulatedBindings.isEmpty()) {
                return Collections.emptyList(); 
            }
        }
        return accumulatedBindings;
    }

    public static List<Bindings> matchTypes(Atom typePattern, Atom typeConcrete, Bindings initialBindings) {
        Objects.requireNonNull(typePattern, "typePattern cannot be null");
        Objects.requireNonNull(typeConcrete, "typeConcrete cannot be null");
        Objects.requireNonNull(initialBindings, "initialBindings cannot be null");

        if (typePattern.equals(MettaSymbols.UNDEF_TYPE)) {
            return Collections.singletonList(initialBindings.copy());
        }
        if (typeConcrete.equals(MettaSymbols.UNDEF_TYPE)) {
            if (typePattern instanceof VariableAtom) {
                Bindings b = initialBindings.copy();
                return b.addValueBinding((VariableAtom) typePattern, MettaSymbols.UNDEF_TYPE) ? Collections.singletonList(b) : Collections.emptyList();
            }
            return typePattern.equals(MettaSymbols.UNDEF_TYPE) ? Collections.singletonList(initialBindings.copy()) : Collections.emptyList();
        }

        List<Bindings> results = new ArrayList<>();
        Atom concretePattern = Matcher.applyBindings(typePattern, initialBindings);
        List<Bindings> matchOutcome = Matcher.matchAtoms(concretePattern, typeConcrete);
        
        for(Bindings b : matchOutcome){
            Bindings merged = initialBindings.copy();
            results.addAll(merged.merge(b)); 
        }
        
        return results.stream().distinct().collect(Collectors.toList());
    }

    public static boolean checkType(SpaceReader space, Atom atom, Atom expectedType) {
        Objects.requireNonNull(atom, "atom cannot be null");
        Objects.requireNonNull(expectedType, "expectedType cannot be null");

        if (expectedType.equals(MettaSymbols.UNDEF_TYPE)) return true;
        if (expectedType.equals(MettaSymbols.ATOM_TYPE)) return true; 
        if (expectedType.equals(MettaSymbols.SYMBOL_TYPE) && atom instanceof SymbolAtom) return true;
        if (expectedType.equals(MettaSymbols.VARIABLE_TYPE) && atom instanceof VariableAtom) return true;
        if (expectedType.equals(MettaSymbols.EXPRESSION_TYPE) && atom instanceof ExpressionAtom) return true;
        if (expectedType.equals(MettaSymbols.GROUNDED_TYPE) && atom instanceof GroundedAtom) return true;
        if (expectedType.equals(MettaSymbols.SPACE_TYPE) && atom instanceof WrappedSpaceAtom) return true;

        List<AtomType> actualTypes = getAtomTypes(space, atom);
        for (AtomType actualType : actualTypes) {
            if (actualType.isError()) continue;
            if (!matchTypes(expectedType, actualType.getAtom(), new Bindings()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean validateAtom(SpaceReader space, Atom atom) {
        List<AtomType> types = getAtomTypes(space, atom);
        if (types.isEmpty()) return false; 
        
        boolean hasNonErrorNonUndefType = false;
        for (AtomType type : types) {
            if (type.isError()) return false; 
            if (!type.getAtom().equals(MettaSymbols.UNDEF_TYPE)) {
                hasNonErrorNonUndefType = true;
            }
        }
        return hasNonErrorNonUndefType;
    }
}
