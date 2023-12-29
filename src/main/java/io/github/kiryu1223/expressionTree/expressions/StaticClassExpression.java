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
}
