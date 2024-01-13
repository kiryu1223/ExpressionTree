package io.github.kiryu1223.expressionTree.expressions;

public class TypeCastExpression extends Expression
{
    private final Class<?> targetType;
    private final Expression expr;

    public TypeCastExpression(Class<?> targetType, Expression expr)
    {
        this.targetType = targetType;
        this.expr = expr;
    }

    public Class<?> getTargetType()
    {
        return targetType;
    }

    public Expression getExpr()
    {
        return expr;
    }

    @Override
    public Kind getKind()
    {
        return Kind.TypeCast;
    }

    @Override
    public String toString()
    {
        return "(" + targetType.getSimpleName() + ")" + expr;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TypeCastExpression that = (TypeCastExpression) obj;
        return targetType.equals(that.targetType) && expr.equals(that.expr);
    }
}
