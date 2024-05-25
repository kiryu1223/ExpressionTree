package io.github.kiryu1223.expressionTree.expressions;

import io.github.kiryu1223.expressionTree.delegate.Delegate;

import java.util.List;

public class LambdaExpression<T extends Delegate> extends Expression
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LambdaExpression<T> that = (LambdaExpression<T>) obj;
        return body.equals(that.body) && parameters.equals(that.parameters)
                && returnType.equals(that.returnType);
    }

//    public T compile()
//    {
//        return DynamicCompilerUtil.dynamicCompile(this);
//    }
}
