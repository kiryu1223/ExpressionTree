package io.github.kiryu1223.expressionTree.expressions;

public class ReturnExpression extends Expression
{
    private final Expression expr;

    public ReturnExpression(Expression expr)
    {
        this.expr = expr;
    }

    public Expression getExpr()
    {
        return expr;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Return;
    }

    @Override
    public String toString()
    {
        return "return " + expr;
    }
}
