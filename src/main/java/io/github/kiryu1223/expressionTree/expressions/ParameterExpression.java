package io.github.kiryu1223.expressionTree.expressions;

public class ParameterExpression extends Expression
{
    private final Class<?> type;
    private final String name;

    public ParameterExpression(Class<?> type, String name)
    {
        this.type = type;
        this.name = name;
    }

    public Class<?> getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Parameter;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
