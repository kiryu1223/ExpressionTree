package io.github.kiryu1223.expressionTree.expressions;

public class ContinueExpression extends Expression
{
    @Override
    public Kind getKind()
    {
        return Kind.Continue;
    }

    @Override
    public String toString()
    {
        return "continue";
    }
}
