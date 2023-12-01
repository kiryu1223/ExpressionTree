package io.github.kiryu1223.expressionTree.expressionV2;

import java.util.List;

public class ForExpression implements IExpression
{
    private final List<IExpression> init;
    private final IExpression cond;
    private final List<IExpression> step;
    private final IExpression body;

    public ForExpression(List<IExpression> init, IExpression cond, List<IExpression> step, IExpression body)
    {
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }

    public List<IExpression> getInit()
    {
        return init;
    }

    public IExpression getCond()
    {
        return cond;
    }

    public List<IExpression> getStep()
    {
        return step;
    }

    public IExpression getBody()
    {
        return body;
    }
}
