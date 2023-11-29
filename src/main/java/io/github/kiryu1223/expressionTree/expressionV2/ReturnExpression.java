package io.github.kiryu1223.expressionTree.expressionV2;

public class ReturnExpression implements IExpression
{
    private final IExpression expression;

    public ReturnExpression(IExpression expression)
    {
        this.expression = expression;
    }

    public IExpression getExpression()
    {
        return expression;
    }
}
