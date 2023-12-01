package io.github.kiryu1223.expressionTree.expressionV2;

public class WhileExpression implements IExpression
{
    private final IExpression cond;
    private final IExpression body;

    public WhileExpression(IExpression cond, IExpression body)
    {
        this.cond = cond;
        this.body = body;
    }

    public IExpression getCond()
    {
        return cond;
    }

    public IExpression getBody()
    {
        return body;
    }
}
