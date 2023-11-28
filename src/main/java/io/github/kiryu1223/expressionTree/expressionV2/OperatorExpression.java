package io.github.kiryu1223.expressionTree.expressionV2;

public abstract class OperatorExpression implements IExpression
{
    protected final Operator operator;

    protected OperatorExpression(Operator operator)
    {
        this.operator = operator;
    }

    public Operator getOperator()
    {
        return operator;
    }

    @Override
    public abstract String toString();
}
