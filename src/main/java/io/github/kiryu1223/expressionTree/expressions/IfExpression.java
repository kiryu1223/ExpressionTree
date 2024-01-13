package io.github.kiryu1223.expressionTree.expressions;

public class IfExpression extends Expression
{
    private final Expression condition;
    private final Expression thenPart;
    private final Expression elsePart;

    public IfExpression(Expression condition, Expression thenPart, Expression elsePart)
    {
        this.condition = condition;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public Expression getThenPart()
    {
        return thenPart;
    }

    public Expression getElsePart()
    {
        return elsePart;
    }

    @Override
    public Kind getKind()
    {
        return Kind.If;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("if ").append(condition).append(" ");
        if (thenPart != null)
        {
            sb.append(thenPart);
            sb.append(thenPart.getKind() == Kind.Block ? "" : ";");
        }
        if (elsePart != null)
        {
            sb.append("\n    ").append("else ").append(elsePart);
            sb.append(elsePart.getKind() == Kind.Block || elsePart.getKind() == Kind.If ? "" : ";");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IfExpression that = (IfExpression) obj;
        return condition.equals(that.condition) && thenPart.equals(that.thenPart)
                && elsePart.equals(that.elsePart);
    }
}
