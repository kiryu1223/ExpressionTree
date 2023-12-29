package io.github.kiryu1223.expressionTree.expressions;

public class AssignOpExpression extends Expression
{
    private final Expression left;
    private final Expression right;
    private final OperatorType operatorType;

    public AssignOpExpression(Expression left, Expression right, OperatorType operatorType)
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
        return Kind.AssignOp;
    }

    @Override
    public String toString()
    {
        return left + " " + operatorType.getOperator() + " " + right;
    }
}
