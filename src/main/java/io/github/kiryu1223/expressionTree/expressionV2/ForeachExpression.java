package io.github.kiryu1223.expressionTree.expressionV2;

public class ForeachExpression implements IExpression
{
    private final VarExpression var;
    private final IExpression expr;
    private final IExpression body;

    public ForeachExpression(VarExpression var, IExpression expr, IExpression body)
    {
        this.var = var;
        this.expr = expr;
        this.body = body;
    }

    public VarExpression getVar()
    {
        return var;
    }

    public IExpression getExpr()
    {
        return expr;
    }

    public IExpression getBody()
    {
        return body;
    }
}
