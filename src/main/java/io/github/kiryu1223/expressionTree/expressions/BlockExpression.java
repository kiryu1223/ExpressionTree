package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class BlockExpression extends Expression
{
    private final List<Expression> expressions;
    private final List<ParameterExpression> variables;

    public BlockExpression(List<Expression> expressions, List<ParameterExpression> variables)
    {
        this.expressions = expressions;
        this.variables = variables;
    }

    public List<Expression> getExpressions()
    {
        return expressions;
    }

    public List<ParameterExpression> getVariables()
    {
        return variables;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Block;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Expression expression : expressions)
        {
            sb.append("    ").append(expression);
            switch (expression.getKind())
            {
                case Block:
                case Lambda:
                case If:
                case Foreach:
                case For:
                case While:
                case Switch:
                case Catch:
                case Try:
                    break;
                default:
                    sb.append(";");
                    break;
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
