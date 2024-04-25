package io.github.kiryu1223.expressionTree.expressions;

import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FieldSelectExpression extends Expression
{
    private final Expression expr;
    private final Field field;

    public FieldSelectExpression(Expression expr, Field field)
    {
        this.expr = expr;
        this.field = field;
    }

    public Expression getExpr()
    {
        return expr;
    }

    public Field getField()
    {
        return field;
    }

    @Override
    public Object getValue()
    {
        if (hasParameterExpression()) return null;
        try
        {
            Object value = expr.getValue();
            return field.get(value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Kind getKind()
    {
        return Kind.FieldSelect;
    }

    @Override
    public String toString()
    {
        return expr + "." + field.getName();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FieldSelectExpression that = (FieldSelectExpression) obj;
        return expr.equals(that.expr) && field.equals(that.field);
    }

    public boolean inParameters(List<ParameterExpression> parameters)
    {
        if (expr instanceof ParameterExpression)
        {
            ParameterExpression parameter = (ParameterExpression) expr;
            return parameters.contains(parameter);
        }
        return false;
    }
}
