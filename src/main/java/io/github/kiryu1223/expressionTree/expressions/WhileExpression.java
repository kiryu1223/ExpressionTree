package io.github.kiryu1223.expressionTree.expressions;


public class WhileExpression extends Expression
{
    private final Expression condition;
    private final Expression body;

    public WhileExpression(Expression condition, Expression body)
    {
        this.condition = condition;
        this.body = body;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public Expression getBody()
    {
        return body;
    }

    @Override
    public Kind getKind()
    {
        return Kind.While;
    }

    @Override
    public String toString()
    {
        return "while " + condition + body;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WhileExpression that = (WhileExpression) obj;
        return condition.equals(that.condition) && body.equals(that.body);
    }
}
