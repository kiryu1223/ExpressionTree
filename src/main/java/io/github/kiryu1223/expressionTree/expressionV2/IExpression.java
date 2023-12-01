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

    public static BreakExpression Break()
    {
        return new BreakExpression();
    }

    public static ContinueExpression Continue()
    {
        return new ContinueExpression();
    }

    public static SwitchExpression Switch(IExpression selector, CaseExpression... cases)
    {
        return new SwitchExpression(selector, Arrays.asList(cases));
    }

    public static CaseExpression Case(IExpression part, IExpression... stats)
    {
        return new CaseExpression(part, Arrays.asList(stats));
    }

    public static ForeachExpression foreach(VarExpression var, IExpression expr, IExpression body)
    {
        return new ForeachExpression(var, expr, body);
    }

    public static WhileExpression While(IExpression cond, IExpression body)
    {
        return new WhileExpression(cond, body);
    }

    public static ConditionalExpression conditional(IExpression cond, IExpression truePart, IExpression falsePart)
    {
        return new ConditionalExpression(cond, truePart, falsePart);
    }

    enum Type
    {
        Binary("binary"), Value("value"), Unary("unary"), New("New"),
        Parens("parens"), FieldSelect("fieldSelect"), MethodCall("methodCall"),
        Reference("reference"), Block("block"), Assign("assign"), Var("var"),
        ArrayAccess("arrayAccess"), If("If"), LocalReference("localReference"),
        Return("Return"), Break("Break"), Continue("Continue"), Switch("Switch"),
        Case("Case"), Foreach("foreach"), While("While"),Conditional("conditional"),
        ;

        private final String methodName;

        Type(String methodName)
        {
            this.methodName = methodName;
        }

        public String getMethodName()
        {
            return methodName;
        }
    }
}
