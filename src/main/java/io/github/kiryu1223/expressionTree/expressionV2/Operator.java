package io.github.kiryu1223.expressionTree.expressionV2;

public enum Operator
{
    /**
     * Unary
     */
    NOT("!"),
    /**
     * Binary
     */
    EQ("=="),
    NE("!="),
    GE(">="),
    LE("<="),
    GT(">"),
    LT("<"),
    AND("&&"),
    OR("||"),
    PLUS("+"),
    MINUS("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),
    ;

    private final String op;

    Operator(String op)
    {
        this.op = op;
    }

    @Override
    public String toString()
    {
        return op;
    }
}
