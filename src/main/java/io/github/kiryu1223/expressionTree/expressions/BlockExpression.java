package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class BlockExpression extends Expression
{
    private final List<Expression> expressions;
    private final List<ParameterExpression> variables;
    private final boolean isStatic;

    public BlockExpression(List<Expression> expressions, List<ParameterExpression> variables, boolean isStatic)
    {
        this.expressions = expressions;
        this.variables = variables;
        this.isStatic = isStatic;
    }

    public List<Expression> getExpressions()
    {
        return expressions;
    }

    public List<ParameterExpression> getVariables()
    {
        return variables;
    }

    public boolean isStatic()
    {
        return isStatic;
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockExpression that = (BlockExpression) obj;
        return expressions.equals(that.expressions) && variables.equals(that.variables);
    }
}
