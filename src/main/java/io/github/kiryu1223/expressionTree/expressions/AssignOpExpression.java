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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AssignOpExpression that = (AssignOpExpression) obj;
        return left.equals(that.left) && right.equals(that.right) && operatorType.equals(that.operatorType);
    }
}
