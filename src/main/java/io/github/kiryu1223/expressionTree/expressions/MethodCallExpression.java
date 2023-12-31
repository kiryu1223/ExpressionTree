package io.github.kiryu1223.expressionTree.expressions;

import java.lang.reflect.Method;
import java.util.List;

public class MethodCallExpression extends Expression
{
    private final Expression expr;
    private final Method method;
    private final List<Expression> args;

    public MethodCallExpression(Expression expr, Method method, List<Expression> args)
    {
        this.expr = expr;
        this.method = method;
        this.args = args;
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
}
