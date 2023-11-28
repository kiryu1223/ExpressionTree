package io.github.kiryu1223.expressionTree.expressionV2;

public class BinaryExpression extends OperatorExpression
{
    private final IExpression left;
    private final IExpression right;

    BinaryExpression(IExpression left, IExpression right, Operator operator)
    {
        super(operator);
        this.left = left;
        this.right = right;
    }

    public IExpression getLeft()
    {
        return left;
    }

    public IExpression getRight()
    {
        return right;
    }

    @Override
    public String toString()
    {
        return String.format("%s %s %s", left, operator, right);
    }
}
