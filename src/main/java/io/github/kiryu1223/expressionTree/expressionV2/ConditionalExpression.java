package io.github.kiryu1223.expressionTree.expressionV2;


public class ConditionalExpression implements IExpression
{
    private final IExpression cond;
    private final IExpression truePart;
    private final IExpression falsePart;

    public ConditionalExpression(IExpression cond, IExpression truePart, IExpression falsePart)
    {
        this.cond = cond;
        this.truePart = truePart;
        this.falsePart = falsePart;
    }

    public IExpression getCond()
    {
        return cond;
    }

    public IExpression getTruePart()
    {
        return truePart;
    }

    public IExpression getFalsePart()
    {
        return falsePart;
    }

    @Override
    public String toString()
    {
        return String.format("%s ? %s : %s", cond, truePart, falsePart);
    }
}
