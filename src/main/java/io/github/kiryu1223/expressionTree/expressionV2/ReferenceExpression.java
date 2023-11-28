package io.github.kiryu1223.expressionTree.expressionV2;

public class ReferenceExpression implements IExpression
{
    private final Object reference;

    public ReferenceExpression(Object reference)
    {
        this.reference = reference;
    }

    public Object getReference()
    {
        return reference;
    }

    @Override
    public String toString()
    {
        return reference.toString();
    }
}
