package io.github.kiryu1223.expressionTree.expressions;

import java.lang.reflect.Field;

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
}
