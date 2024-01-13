package io.github.kiryu1223.expressionTree.expressions;

public class ConditionalExpression extends Expression
{
    private final Expression condition;
    private final Expression truePart;
    private final Expression falsePart;

    public ConditionalExpression(Expression condition, Expression truePart, Expression falsePart)
    {
        this.condition = condition;
        this.truePart = truePart;
        this.falsePart = falsePart;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public Expression getTruePart()
    {
        return truePart;
    }

    public Expression getFalsePart()
    {
        return falsePart;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Conditional;
    }

    @Override
    public String toString()
    {
        return condition + " ? " + truePart + " : " + falsePart;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConditionalExpression that = (ConditionalExpression) obj;
        return condition.equals(that.condition)
                && truePart.equals(that.truePart)
                && falsePart.equals(that.falsePart);
    }
}
