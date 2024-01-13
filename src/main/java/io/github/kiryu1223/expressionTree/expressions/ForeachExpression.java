package io.github.kiryu1223.expressionTree.expressions;

public class ForeachExpression extends Expression
{
    private final VariableExpression var;
    private final Expression expr;
    private final Expression body;

    public ForeachExpression(VariableExpression var, Expression expr, Expression body)
    {
        this.var = var;
        this.expr = expr;
        this.body = body;
    }

    public VariableExpression getVar()
    {
        return var;
    }

    public Expression getExpr()
    {
        return expr;
    }

    public Expression getBody()
    {
        return body;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Foreach;
    }

    @Override
    public String toString()
    {
        return "for (" + var + " : " + expr + ")" + body;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ForeachExpression that = (ForeachExpression) obj;
        return var.equals(that.var) && expr.equals(that.expr) && body.equals(that.body);
    }
}
