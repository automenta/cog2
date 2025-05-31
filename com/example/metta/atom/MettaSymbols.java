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

    /** Symbol representing the generic type for all LinkAtoms, analogous to ATOM_TYPE for all atoms. */
    public static final Atom LINK_TYPE_SYMBOL = new SymbolAtom("Link");
    /** Symbol representing a link type for expressions that evaluate to a value (e.g., (EvaluationLink (foo bar) result)). */
    public static final Atom EVALUATION_LINK_SYMBOL = new SymbolAtom("EvaluationLink");
    /** Symbol representing an inheritance relationship between types or concepts (e.g., (InheritanceLink cat mammal)). */
    public static final Atom INHERITANCE_LINK_SYMBOL = new SymbolAtom("InheritanceLink");
    /** Symbol representing a similarity relationship between atoms (e.g., (SimilarityLink apple orange)). */
    public static final Atom SIMILARITY_LINK_SYMBOL = new SymbolAtom("SimilarityLink");
    /** Symbol representing a membership relationship (e.g., (MemberLink a_specific_cat cat_species)). */
    public static final Atom MEMBER_LINK_SYMBOL = new SymbolAtom("MemberLink");
    /** Symbol representing an instantiation relationship (e.g., (InstanceLink my_cat Cat)). */
    public static final Atom INSTANCE_LINK_SYMBOL = new SymbolAtom("InstanceLink");

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

    /** Symbol used to define an implication rule (e.g., (=> (And (premise1) (premise2)) (conclusion))). */
    public static final Atom IMPLIES_SYMBOL = new SymbolAtom("=>");

    /** Symbol used to represent a conjunction of conditions in a rule or query. */
    public static final Atom AND_SYMBOL = new SymbolAtom("And");
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

    /** Symbol used for graph traversal queries, e.g., (Traverse $startNode (Path (LinkA $startNode $mid) (LinkB $mid $endNode)) $endNode). */
    public static final Atom TRAVERSE_SYMBOL = new SymbolAtom("Traverse");
    /** Symbol used within a Traverse query to specify a sequence of link patterns to be traversed. */
    public static final Atom PATH_SYMBOL = new SymbolAtom("Path");

    /** Symbol used for type annotation or declaration (e.g., (isa $x Type)). */
    public static final Atom COLON_SYMBOL = new SymbolAtom(":");
}
