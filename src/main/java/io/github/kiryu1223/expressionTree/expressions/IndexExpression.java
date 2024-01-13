package io.github.kiryu1223.expressionTree.expressions;

public class IndexExpression extends Expression
{
    private final Expression object;
    private final Expression index;

    public IndexExpression(Expression object, Expression index)
    {
        this.object = object;
        this.index = index;
    }

    public Expression getObject()
    {
        return object;
    }

    public Expression getIndex()
    {
        return index;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Index;
    }

    @Override
    public String toString()
    {
        return object + "[" + index + "]";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IndexExpression that = (IndexExpression) obj;
        return object.equals(that.object) && index.equals(that.index);
    }
}
