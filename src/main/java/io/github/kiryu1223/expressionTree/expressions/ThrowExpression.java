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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThrowExpression that = (ThrowExpression) obj;
        return expr.equals(that.expr);
    }
}
