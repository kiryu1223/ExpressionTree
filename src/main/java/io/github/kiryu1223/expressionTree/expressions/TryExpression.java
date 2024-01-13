package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class TryExpression extends Expression
{
    private final BlockExpression body;
    private final List<CatchExpression> catchers;
    private final BlockExpression finalizer;
    private final List<Expression> resources;

    public TryExpression(BlockExpression body, List<CatchExpression> catchers, BlockExpression finalizer, List<Expression> resources)
    {
        this.body = body;
        this.catchers = catchers;
        this.finalizer = finalizer;
        this.resources = resources;
    }

    public BlockExpression getBody()
    {
        return body;
    }

    public List<CatchExpression> getCatchers()
    {
        return catchers;
    }

    public BlockExpression getFinalizer()
    {
        return finalizer;
    }

    public List<Expression> getResources()
    {
        return resources;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Try;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("try");
        if (!resources.isEmpty())
        {
            sb.append("(");
            for (Expression resource : resources)
            {
                sb.append(resource).append(";");
            }
            sb.append(")");
        }
        if(body!=null)
        {
            sb.append(body);
        }
        for (CatchExpression catcher : catchers)
        {
            sb.append(catcher);
        }
        if(finalizer!=null)
        {
            sb.append("finally").append(finalizer);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TryExpression that = (TryExpression) obj;
        return body.equals(that.body)&&catchers.equals(that.catchers)
                &&finalizer.equals(that.finalizer)&&resources.equals(that.resources);
    }
}
