package io.github.kiryu1223.expressionTree.expressions;

public class CatchExpression extends Expression
{
    private final VariableExpression param;
    private final BlockExpression body;

    public CatchExpression(VariableExpression param, BlockExpression body)
    {
        this.param = param;
        this.body = body;
    }

    public VariableExpression getParam()
    {
        return param;
    }

    public BlockExpression getBody()
    {
        return body;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Catch;
    }

    @Override
    public String toString()
    {
        return "catch (" + param + ")" + body;
    }
}
