package io.github.kiryu1223.expressionTree.expressionV2;

public class IfExpression implements IExpression
{
    private final IExpression cond;
    private final IExpression body;
    private final IExpression elSe;

    public IfExpression(IExpression cond, IExpression body, IExpression elSe)
    {
        this.cond = cond;
        this.body = body;
        this.elSe = elSe;
    }

    public IExpression getCond()
    {
        return cond;
    }

    public IExpression getBody()
    {
        return body;
    }

    public IExpression getElSe()
    {
        return elSe;
    }
}
