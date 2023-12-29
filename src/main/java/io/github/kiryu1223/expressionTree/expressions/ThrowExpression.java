package io.github.kiryu1223.expressionTree.expressions;

public class ThrowExpression extends Expression
{
    private final Expression expr;

    public ThrowExpression(Expression expr)
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
        return Kind.Throw;
    }

    @Override
    public String toString()
    {
        return "throw " + expr;
    }
}
