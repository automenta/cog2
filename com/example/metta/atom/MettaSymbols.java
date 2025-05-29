package com.example.metta.atom;

public final class MettaSymbols {

    private MettaSymbols() {
    }

    public static final Atom UNDEF_TYPE = new SymbolAtom("%Undefined%");
    public static final Atom TYPE_TYPE = new SymbolAtom("Type");
    public static final Atom UNIT_TYPE = new SymbolAtom("Unit"); 

    public static final Atom ATOM_TYPE = new SymbolAtom("Atom");
    public static final Atom SYMBOL_TYPE = new SymbolAtom("Symbol");
    public static final Atom VARIABLE_TYPE = new SymbolAtom("Variable");
    public static final Atom EXPRESSION_TYPE = new SymbolAtom("Expression");
    public static final Atom GROUNDED_TYPE = new SymbolAtom("Grounded");
    public static final Atom SPACE_TYPE = new SymbolAtom("Space"); 

    public static final Atom ARROW_SYMBOL = new SymbolAtom("->");
    public static final Atom EQUAL_SYMBOL = new SymbolAtom("=");
    public static final Atom ERROR_SYMBOL = new SymbolAtom("Error");
    public static final Atom COMMA_SYMBOL = new SymbolAtom(",");

    public static final Atom EVAL_SYMBOL = new SymbolAtom("eval");
    public static final Atom CHAIN_SYMBOL = new SymbolAtom("chain");
    public static final Atom UNIFY_SYMBOL = new SymbolAtom("unify");
    public static final Atom DECONS_SYMBOL = new SymbolAtom("decons");
    public static final Atom CONS_SYMBOL = new SymbolAtom("cons");
    public static final Atom FUNCTION_SYMBOL = new SymbolAtom("function");
    public static final Atom RETURN_SYMBOL = new SymbolAtom("return");
    public static final Atom METTA_SYMBOL = new SymbolAtom("metta");
    public static final Atom COLLAPSE_BIND_SYMBOL = new SymbolAtom("collapse-bind");
    public static final Atom SUPERPOSE_BIND_SYMBOL = new SymbolAtom("superpose-bind");

    public static final Atom COLON_SYMBOL = new SymbolAtom(":");
}
