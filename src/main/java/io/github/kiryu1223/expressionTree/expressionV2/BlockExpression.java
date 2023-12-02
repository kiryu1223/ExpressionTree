package io.github.kiryu1223.expressionTree.expressionV2;

import java.util.List;

public class BlockExpression implements IExpression
{
    private final List<IExpression> expressions;

    BlockExpression(List<IExpression> expressions)
    {
        this.expressions = expressions;
    }

    public List<IExpression> getExpressions()
    {
        return expressions;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (IExpression expression : expressions)
        {
            sb.append("    ")
                    .append(expression)
                    .append(";")
                    .append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
