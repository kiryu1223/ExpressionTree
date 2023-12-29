package io.github.kiryu1223.expressionTree.expressions;


import java.util.List;

public class CaseExpression extends Expression
{
    private final Expression part;
    private final List<Expression> stats;

    public CaseExpression(Expression part, List<Expression> stats)
    {
        this.part = part;
        this.stats = stats;
    }

    public Expression getPart()
    {
        return part;
    }

    public List<Expression> getStats()
    {
        return stats;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Case;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("case " + part + ":");
        for (Expression stat : stats)
        {
            sb.append("\n    ").append(stat);
            switch (stat.getKind())
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
        }
        return sb.toString();
    }
}
