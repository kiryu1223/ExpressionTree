package io.github.kiryu1223.expressionTree.expressions;

public class AssignExpression extends Expression
{
    private final Expression left;
    private final Expression right;

    public AssignExpression(Expression left, Expression right)
    {
        this.left = left;
        this.right = right;
    }

    public Expression getLeft()
    {
        return left;
    }

    public Expression getRight()
    {
        return right;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Assign;
    }

    @Override
    public String toString()
    {
        return left + " = " + right;
    }
}
