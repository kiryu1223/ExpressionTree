package io.github.kiryu1223.expressionTree.expressionV2;

import java.util.ArrayList;
import java.util.Arrays;

public interface IExpression
{
    public static BinaryExpression binary(IExpression left, IExpression right, Operator operator)
    {
        return new BinaryExpression(left, right, operator);
    }

    public static <T> ValueExpression<T> value(T value)
    {
        return new ValueExpression<T>(value);
    }

    public static UnaryExpression unary(IExpression expression, Operator operator)
    {
        return new UnaryExpression(expression, operator);
    }

    public static <T> NewExpression<T> New(Class<T> target, IExpression... expressions)
    {
        return new NewExpression<T>(target, Arrays.asList(expressions));
    }

    public static <T> NewExpression<T> New(T t, IExpression... expressions)
    {
        return new NewExpression<T>((Class<T>) t.getClass(), Arrays.asList(expressions));
    }

    public static ParensExpression parens(IExpression expression)
    {
        return new ParensExpression(expression);
    }

    public static FieldSelectExpression fieldSelect(IExpression selector, String selected)
    {
        return new FieldSelectExpression(selector, selected);
    }

    public static MethodCallExpression methodCall(IExpression selector, String selected, IExpression... params)
    {
        return new MethodCallExpression(selector, selected, new ArrayList<>(Arrays.asList(params)));
    }

    public static ReferenceExpression reference(Object t)
    {
        return new ReferenceExpression(t);
    }

    public static BlockExpression block(IExpression... expression)
    {
        return new BlockExpression(Arrays.asList(expression));
    }

    public static AssignExpression assign(IExpression left, IExpression right)
    {
        return new AssignExpression(left, right);
    }

    public static VarExpression var(String name, IExpression init)
    {
        return new VarExpression(name, init);
    }

    public static ArrayAccessExpression arrayAccess(IExpression indexed, IExpression index)
    {
        return new ArrayAccessExpression(indexed, index);
    }

    public static IfExpression If(IExpression condition, IExpression body, IExpression elSe)
    {
        return new IfExpression(condition, body, elSe);
    }

    public static LocalReferenceExpression localReference(String name)
    {
        return new LocalReferenceExpression(name);
    }
    public static ReturnExpression Return(IExpression expression)
    {
        return new ReturnExpression(expression);
    }
    enum Type
    {
        Binary, Value, Unary, New, Parens, FieldSelect, MethodCall,
        Reference, Block, Assign, Var, ArrayAccess, If, LocalReference,
        Return,
        ;
    }
}
