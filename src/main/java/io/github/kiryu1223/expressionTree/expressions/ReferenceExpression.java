package io.github.kiryu1223.expressionTree.expressions;

public class ReferenceExpression extends Expression
{
    private final Object ref;
    private final String name;
    private final boolean isPrimitive;

    public ReferenceExpression(Object ref, String name, boolean isPrimitive)
    {
        this.ref = ref;
        this.name = name;
        this.isPrimitive = isPrimitive;
    }

    public Object getRef()
    {
        return ref;
    }

    public String getName()
    {
        return name;
    }

    public boolean isPrimitive()
    {
        return isPrimitive;
    }

    @Override
    public Object getValue()
    {
        return getRef();
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ReferenceExpression that = (ReferenceExpression) obj;
        return name.equals(that.name) && ref.getClass().equals(that.ref.getClass());
    }
}
