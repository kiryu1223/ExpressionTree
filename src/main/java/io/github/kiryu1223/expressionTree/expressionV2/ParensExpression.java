package io.github.kiryu1223.expressionTree.expressionV2;

public class ParensExpression implements IExpression
{
    private final IExpression expression;

    ParensExpression(IExpression expression)
    {
        this.expression = expression;
    }

    public IExpression getExpression()
    {
        return expression;
    }

    @Override
    public String toString()
    {
        return String.format("(%s)", expression);
    }
}
