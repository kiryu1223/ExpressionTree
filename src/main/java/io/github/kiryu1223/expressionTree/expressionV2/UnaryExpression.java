package io.github.kiryu1223.expressionTree.expressionV2;

public class UnaryExpression extends OperatorExpression
{
    private final IExpression expression;

    UnaryExpression(IExpression expression, Operator operator)
    {
        super(operator);
        this.expression = expression;
    }

    public IExpression getExpression()
    {
        return expression;
    }

    @Override
    public String toString()
    {
        return String.format("%s%s", operator, expression);
    }
}
