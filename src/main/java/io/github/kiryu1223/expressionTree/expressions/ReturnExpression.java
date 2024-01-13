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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ReturnExpression that = (ReturnExpression) obj;
        return expr.equals(that.expr);
    }
}
