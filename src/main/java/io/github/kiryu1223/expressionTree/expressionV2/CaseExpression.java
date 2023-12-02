package io.github.kiryu1223.expressionTree.expressionV2;

import java.util.List;

public class CaseExpression implements IExpression
{
    private final IExpression part;
    private final List<IExpression> stats;

    public CaseExpression(IExpression part, List<IExpression> stats)
    {
        this.part = part;
        this.stats = stats;
    }

    public IExpression getPart()
    {
        return part;
    }

    public List<IExpression> getStats()
    {
        return stats;
    }

    @Override
    public String toString()
    {
        StringBuilder sb=new StringBuilder();
        sb.append("case")
                .append(" ")
                .append(part)
                .append(" ")
                .append(":")
                .append("\n");
        for (IExpression stat : stats)
        {
            sb.append(stat).append("\n");
        }
        return sb.toString();
    }
}
