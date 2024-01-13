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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ParensExpression that = (ParensExpression) obj;
        return expr.equals(that.expr);
    }
}
