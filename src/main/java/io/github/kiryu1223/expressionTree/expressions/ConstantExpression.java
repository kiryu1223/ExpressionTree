package io.github.kiryu1223.expressionTree.expressions;

public class ConstantExpression extends Expression
{
    private final Object value;
    private final Class<?> type;

    public ConstantExpression(Object value)
    {
        this.value = value;
        this.type = value == null ? Void.class : value.getClass();
    }

    public Object getValue()
    {
        return value;
    }

    public Class<?> getType()
    {
        return type;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Constant;
    }

    @Override
    public String toString()
    {
        if (type == Character.TYPE || type == Character.class)
        {
            return "'" + value + "'";
        }
        else if (type == String.class)
        {
            return "\"" + value + "\"";
        }
        else
        {
            return value.toString();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConstantExpression that = (ConstantExpression) obj;
        if (that.value == null && type == Void.class)
        {
            return true;
        }
        else
        {
            return value.equals(that.value);
        }
    }

}
