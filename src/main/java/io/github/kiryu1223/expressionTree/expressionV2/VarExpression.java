package io.github.kiryu1223.expressionTree.expressionV2;

public class VarExpression implements IExpression
{
    private final String name;
    private final IExpression init;

    VarExpression(String name, IExpression init)
    {
        this.name = name;
        this.init = init;
    }

    public String getName()
    {
        return name;
    }

    public IExpression getInit()
    {
        return init;
    }
}
