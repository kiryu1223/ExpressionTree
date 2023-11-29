package io.github.kiryu1223.expressionTree.expressionV2;

public class IfExpression implements IExpression
{
    private final IExpression condition;
    private final IExpression body;
    private final IExpression elSe;

    public IfExpression(IExpression condition, IExpression body, IExpression elSe)
    {
        this.condition = condition;
        this.body = body;
        this.elSe = elSe;
    }

    public IExpression getCondition()
    {
        return condition;
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
