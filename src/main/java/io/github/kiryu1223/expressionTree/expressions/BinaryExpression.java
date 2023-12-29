package io.github.kiryu1223.expressionTree.expressions;

public class BinaryExpression extends Expression
{
    private final Expression left;
    private final Expression right;
    private final OperatorType operatorType;

    public BinaryExpression(Expression left, Expression right, OperatorType operatorType)
    {
        this.left = left;
        this.right = right;
        this.operatorType = operatorType;
    }

    public Expression getLeft()
    {
        return left;
    }

    public Expression getRight()
    {
        return right;
    }

    public OperatorType getOperatorType()
    {
        return operatorType;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Binary;
    }

    @Override
    public String toString()
    {
        return left + " " + operatorType.getOperator() + " " + right;
    }
}
