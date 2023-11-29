package io.github.kiryu1223.expressionTree.expressionV2;

public class LocalReferenceExpression implements IExpression
{
    private final String name;

    public LocalReferenceExpression(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
