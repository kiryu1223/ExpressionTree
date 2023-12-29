package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class LambdaExpression extends Expression
{
    private final Expression body;
    private final List<ParameterExpression> parameters;
    private final Class<?> returnType;

    public LambdaExpression(Expression body, List<ParameterExpression> parameters, Class<?> returnType)
    {
        this.body = body;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public Expression getBody()
    {
        return body;
    }

    public List<ParameterExpression> getParameters()
    {
        return parameters;
    }

    public Class<?> getReturnType()
    {
        return returnType;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Lambda;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (ParameterExpression parameter : parameters)
        {
            sb.append(parameter.getName()).append(",");
        }
        if (sb.charAt(sb.length() - 1) == ',')
        {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(") -> ");
        sb.append(body);
        return sb.toString();
    }
}
