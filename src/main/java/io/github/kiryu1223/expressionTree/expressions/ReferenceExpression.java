package io.github.kiryu1223.expressionTree.expressions;

public class ReferenceExpression extends Expression
{
    private final Object ref;
    private final String name;

    public ReferenceExpression(Object ref, String name)
    {
        this.ref = ref;
        this.name = name;
    }

    public Object getRef()
    {
        return ref;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Reference;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
