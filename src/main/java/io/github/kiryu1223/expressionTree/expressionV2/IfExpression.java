package io.github.kiryu1223.expressionTree.expressionV2;

public class IfExpression implements IExpression
{
    private final IExpression cond;
    private final IExpression body;
    private final IExpression elSe;

    public IfExpression(IExpression cond, IExpression body, IExpression elSe)
    {
        this.cond = cond;
        this.body = body;
        this.elSe = elSe;
    }

    public IExpression getCond()
    {
        return cond;
    }

    public IExpression getBody()
    {
        return body;
    }

    public IExpression getElSe()
    {
        return elSe;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("if(%s)", cond))
                .append("\n")
                .append(body)
                .append("\n");
        if (elSe != null)
        {
            sb.append("else").append("\n").append(elSe);
        }
        return sb.toString();
    }
}
