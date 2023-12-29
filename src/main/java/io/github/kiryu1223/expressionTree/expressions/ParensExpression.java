package io.github.kiryu1223.expressionTree.expressions;

public class ParensExpression extends Expression
{
    private final Expression expr;

    public ParensExpression(Expression expr)
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
        return Kind.Parens;
    }

    @Override
    public String toString()
    {
        return "(" + expr + ")";
    }
}
