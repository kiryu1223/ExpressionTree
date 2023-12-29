package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class SwitchExpression extends Expression
{
    private final Expression selector;
    private final List<CaseExpression> cases;

    public SwitchExpression(Expression selector, List<CaseExpression> cases)
    {
        this.selector = selector;
        this.cases = cases;
    }

    public Expression getSelector()
    {
        return selector;
    }

    public List<CaseExpression> getCases()
    {
        return cases;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Switch;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("switch ").append(selector).append(" ").append("{\n");
        for (CaseExpression aCase : cases)
        {
            sb.append("    ").append(aCase).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
