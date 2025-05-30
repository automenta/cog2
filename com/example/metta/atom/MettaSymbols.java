package com.example.metta.atom;

/**
 * Defines a collection of commonly used {@link SymbolAtom} constants within the Metta system.
 * These symbols represent core types, syntactic elements, and interpreter operations.
 * This class is not meant to be instantiated.
 */
public final class MettaSymbols {

    private MettaSymbols() {
    }

    /** A special symbol representing an undefined type or value. */
    public static final Atom UNDEF_TYPE = new SymbolAtom("%Undefined%");
    /** The symbol representing the type of types themselves. */
    public static final Atom TYPE_TYPE = new SymbolAtom("Type");
    /** A symbol representing a unit type, often used for actions or expressions that produce no specific value. */
    public static final Atom UNIT_TYPE = new SymbolAtom("Unit"); 

    /** The symbol representing the generic Atom type. */
    public static final Atom ATOM_TYPE = new SymbolAtom("Atom");
    /** The symbol representing the SymbolAtom type. */
    public static final Atom SYMBOL_TYPE = new SymbolAtom("Symbol");
    /** The symbol representing the VariableAtom type. */
    public static final Atom VARIABLE_TYPE = new SymbolAtom("Variable");
    /** The symbol representing the ExpressionAtom type. */
    public static final Atom EXPRESSION_TYPE = new SymbolAtom("Expression");
    /** The symbol representing the GroundedAtom type. */
    public static final Atom GROUNDED_TYPE = new SymbolAtom("Grounded");
    /** The symbol representing a Space type. */
    public static final Atom SPACE_TYPE = new SymbolAtom("Space"); 

    /** Symbol used to denote function types (e.g., (-> A B)) or implications. */
    public static final Atom ARROW_SYMBOL = new SymbolAtom("->");
    /** Symbol representing an equality assertion or operation. */
    public static final Atom EQUAL_SYMBOL = new SymbolAtom("=");
    /** Symbol used to denote error states or error atoms. */
    public static final Atom ERROR_SYMBOL = new SymbolAtom("Error");
    /** Symbol used as a separator, often in function arguments or type definitions. */
    public static final Atom COMMA_SYMBOL = new SymbolAtom(",");

    /** Symbol representing the evaluation operation. */
    public static final Atom EVAL_SYMBOL = new SymbolAtom("eval");
    /** Symbol representing a chained evaluation or sequence of operations. */
    public static final Atom CHAIN_SYMBOL = new SymbolAtom("chain");
    /** Symbol representing the unification operation. */
    public static final Atom UNIFY_SYMBOL = new SymbolAtom("unify");
    /** Symbol representing a deconstruction operation on expressions. */
    public static final Atom DECONS_SYMBOL = new SymbolAtom("decons");
    /** Symbol representing a construction operation, typically for expressions. */
    public static final Atom CONS_SYMBOL = new SymbolAtom("cons");
    /** Symbol used to define or denote a function. */
    public static final Atom FUNCTION_SYMBOL = new SymbolAtom("function");
    /** Symbol used to indicate a return operation or a result from an evaluation. */
    public static final Atom RETURN_SYMBOL = new SymbolAtom("return");
    /** Symbol representing the Metta language or a Metta space/interpreter instance itself. */
    public static final Atom METTA_SYMBOL = new SymbolAtom("metta");
    /** Symbol representing an operation to collapse bindings. */
    public static final Atom COLLAPSE_BIND_SYMBOL = new SymbolAtom("collapse-bind");
    /** Symbol representing an operation to superpose bindings. */
    public static final Atom SUPERPOSE_BIND_SYMBOL = new SymbolAtom("superpose-bind");

    /** Symbol used for type annotation or declaration (e.g., (isa $x Type)). */
    public static final Atom COLON_SYMBOL = new SymbolAtom(":");
}
