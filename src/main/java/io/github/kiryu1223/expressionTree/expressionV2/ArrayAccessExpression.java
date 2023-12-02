package io.github.kiryu1223.expressionTree.expressionV2;

public class ArrayAccessExpression implements IExpression
{
    private final IExpression indexed;
    private final IExpression index;

    public ArrayAccessExpression(IExpression indexed, IExpression index)
    {
        this.indexed = indexed;
        this.index = index;
    }

    public IExpression getIndexed()
    {
        return indexed;
    }

    public IExpression getIndex()
    {
        return index;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]",indexed,index);
    }
}
