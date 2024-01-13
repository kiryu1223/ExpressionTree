package io.github.kiryu1223.expressionTree.expressions;

public class StaticClassExpression extends Expression
{
    private final Class<?> type;

    public StaticClassExpression(Class<?> type)
    {
        this.type = type;
    }

    public Class<?> getType()
    {
        return type;
    }

    @Override
    public Kind getKind()
    {
        return Kind.StaticClass;
    }

    @Override
    public String toString()
    {
        return type.getSimpleName();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StaticClassExpression that = (StaticClassExpression) obj;
        return type.equals(that.type);
    }
}
