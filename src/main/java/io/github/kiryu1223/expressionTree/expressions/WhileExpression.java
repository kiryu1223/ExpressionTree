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
}
