package io.github.kiryu1223.expressionTree.expressions;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AssignExpression that = (AssignExpression) obj;
        return left.equals(that.left) && right.equals(that.right);
    }
}
