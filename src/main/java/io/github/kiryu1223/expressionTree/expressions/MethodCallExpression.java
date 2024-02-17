package io.github.kiryu1223.expressionTree.expressions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class MethodCallExpression extends Expression
{
    private final Expression expr;
    private final Method method;
    private final List<Expression> args;
    private final OperatorType operatorType;

    public MethodCallExpression(Expression expr, Method method, List<Expression> args, OperatorType operatorType)
    {
        this.expr = expr;
        this.method = method;
        this.args = args;
        this.operatorType = operatorType;
    }

    public Expression getExpr()
    {
        return expr;
    }

    public Method getMethod()
    {
        return method;
    }

    public List<Expression> getArgs()
    {
        return args;
    }

    @Override
    public Object getValue()
    {
        if (hasParameterExpression()) return null;
        try
        {
            Object thiz = expr.getValue();
            Object[] values = args.stream()
                    .map(m -> m.getValue())
                    .toArray();
            return method.invoke(thiz, values);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    public OperatorType getOperatorType()
    {
        return operatorType;
    }

    @Override
    public Kind getKind()
    {
        return Kind.MethodCall;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if (expr != null)
        {
            sb.append(expr).append(".");
        }
        sb.append(method.getName()).append("(");
        for (Expression arg : args)
        {
            sb.append(arg);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MethodCallExpression that = (MethodCallExpression) obj;
        return expr.equals(that.expr) && method.equals(that.method)
                && args.equals(that.args);
    }

    public boolean inParameters(List<ParameterExpression> parameters)
    {
        if (expr instanceof ParameterExpression)
        {
            ParameterExpression parameter = (ParameterExpression) expr;
            return parameters.contains(parameter);
        }
        return false;
    }
}
