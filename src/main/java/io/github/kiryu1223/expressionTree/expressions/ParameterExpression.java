package io.github.kiryu1223.expressionTree.expressions;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ParameterExpression that = (ParameterExpression) obj;
        return type.equals(that.type) && name.equals(that.name);
    }
}
