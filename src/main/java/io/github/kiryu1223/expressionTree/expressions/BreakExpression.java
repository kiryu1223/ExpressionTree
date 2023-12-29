package io.github.kiryu1223.expressionTree.expressions;

public class BreakExpression extends Expression
{
    @Override
    public Kind getKind()
    {
        return Kind.Break;
    }

    @Override
    public String toString()
    {
        return "break";
    }
}
