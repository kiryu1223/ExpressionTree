package io.github.kiryu1223.expressionTree.expressionV2;

import java.util.List;

public class SwitchExpression implements IExpression
{
    private final IExpression selector;
    private final List<CaseExpression> cases;

    public SwitchExpression(IExpression selector, List<CaseExpression> cases)
    {
        this.selector = selector;
        this.cases = cases;
    }

    public IExpression getSelector()
    {
        return selector;
    }

    public List<CaseExpression> getCases()
    {
        return cases;
    }
}
