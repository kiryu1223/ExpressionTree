package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class NewArrayExpression extends Expression
{
    private final Class<?> type;
    private final List<Expression> counts;
    private final List<Expression> elems;

    public NewArrayExpression(Class<?> type, List<Expression> counts, List<Expression> elems)
    {
        this.type = type;
        this.counts = counts;
        this.elems = elems;
    }

    public Class<?> getType()
    {
        return type;
    }

    public List<Expression> getCounts()
    {
        return counts;
    }

    public List<Expression> getElems()
    {
        return elems;
    }

    @Override
    public Kind getKind()
    {
        return Kind.NewArray;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("new ").append(type.getSimpleName());
        if(!counts.isEmpty())
        {
            for (Expression count : counts)
            {
                sb.append("[").append(count).append("]");
            }
        }
        else
        {
            sb.append("[]");
        }
        if(!elems.isEmpty())
        {
            sb.append("{");
            for (Expression elem : elems)
            {
                sb.append(elem).append(",");
            }
            if (sb.charAt(sb.length() - 1) == ',')
            {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NewArrayExpression that = (NewArrayExpression) obj;
        return type.equals(that.type) && counts.equals(that.counts)
                && elems.equals(that.elems);
    }
}
