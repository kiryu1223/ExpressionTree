package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class ForExpression extends Expression
{
    private final List<Expression> init;
    private final Expression condition;
    private final List<Expression> step;
    private final Expression body;

    public ForExpression(List<Expression> init, Expression condition, List<Expression> step, Expression body)
    {
        this.init = init;
        this.condition = condition;
        this.step = step;
        this.body = body;
    }

    public List<Expression> getInit()
    {
        return init;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public List<Expression> getStep()
    {
        return step;
    }

    public Expression getBody()
    {
        return body;
    }

    @Override
    public Kind getKind()
    {
        return Kind.For;
    }

    @Override
    public String toString()
    {
        // todo:多条件for循环未写
        StringBuilder sb = new StringBuilder();
        sb.append("for (");
        for (Expression expression : init)
        {
            sb.append(expression);
        }
        sb.append(";");
        if (condition != null)
        {
            sb.append(condition);
        }
        sb.append(";");
        for (Expression expression : step)
        {
            sb.append(expression);
        }
        sb.append(") ").append(body);
        return sb.toString();
    }
}
