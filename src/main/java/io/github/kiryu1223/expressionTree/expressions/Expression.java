package io.github.kiryu1223.expressionTree.expressions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

public abstract class Expression
{
    public abstract Kind getKind();

    public abstract String toString();

    public abstract boolean equals(Object obj);

    public void accept(Visitor visitor)
    {
        visitor.visit(this);
    }

    public <V> void accept(GenericsVisitor<V> visitor, V v)
    {
        visitor.visit(this, v);
    }

    public void accept(DeepFindVisitor visitor)
    {
        visitor.visit(this);
    }

    public static BinaryExpression Binary(Expression left, Expression right, OperatorType operatorType)
    {
        return new BinaryExpression(left, right, operatorType);
    }

    public static UnaryExpression Unary(Expression operand, OperatorType operatorType)
    {
        return new UnaryExpression(operand, operatorType);
    }

    public static ConstantExpression Constant(Object value)
    {
        return new ConstantExpression(value);
    }

    public static FieldSelectExpression FieldSelect(Expression expr, Field field)
    {
        return new FieldSelectExpression(expr, field);
    }

    public static MethodCallExpression MethodCall(Expression expr, Method method, Expression[] args)
    {
        return new MethodCallExpression(expr, method, Arrays.asList(args));
    }

    public static ParameterExpression Parameter(Class<?> type, String name)
    {
        return new ParameterExpression(type, name);
    }
    public static NewExpression New(Class<?> type, Expression[] constructorArgs, BlockExpression body)
    {
        return new NewExpression(type, Arrays.asList(constructorArgs), body);
    }

    public static LambdaExpression Lambda(Expression body, ParameterExpression[] parameters, Class<?> returnType)
    {
        return new LambdaExpression(body, Arrays.asList(parameters), returnType);
    }

    public static BlockExpression Block(Expression[] expressions, ParameterExpression[] variables)
    {
        return new BlockExpression(Arrays.asList(expressions), Arrays.asList(variables));
    }

    public static BlockExpression Block(Expression[] expressions)
    {
        return new BlockExpression(Arrays.asList(expressions), Collections.emptyList());
    }

    public static VariableExpression Variable(ParameterExpression parameter, Expression init)
    {
        return new VariableExpression(parameter, init);
    }

    public static VariableExpression Variable(ParameterExpression parameter)
    {
        return Variable(parameter, null);
    }

    public static NewArrayExpression NewArray(Class<?> type, Expression[] counts, Expression[] elems)
    {
        return new NewArrayExpression(type, Arrays.asList(counts), Arrays.asList(elems));
    }

    public static IndexExpression Index(Expression object, Expression index)
    {
        return new IndexExpression(object, index);
    }

    public static AssignExpression Assign(Expression left, Expression right)
    {
        return new AssignExpression(left, right);
    }

    public static AssignOpExpression AssignOp(Expression left, Expression right, OperatorType operatorType)
    {
        return new AssignOpExpression(left, right, operatorType);
    }

    public static StaticClassExpression StaticClass(Class<?> type)
    {
        return new StaticClassExpression(type);
    }

    public static ReferenceExpression Reference(Object ref, String name)
    {
        return new ReferenceExpression(ref, name, false);
    }

    public static ReferenceExpression Reference(Object ref, String name, boolean isPrimitive)
    {
        return new ReferenceExpression(ref, name, isPrimitive);
    }

    public static ReturnExpression Return(Expression expr)
    {
        return new ReturnExpression(expr);
    }

    public static BreakExpression Break()
    {
        return new BreakExpression();
    }

    public static ContinueExpression Continue()
    {
        return new ContinueExpression();
    }

    public static ConditionalExpression Conditional(Expression condition, Expression truePart, Expression falsePart)
    {
        return new ConditionalExpression(condition, truePart, falsePart);
    }

    public static IfExpression If(Expression condition, Expression thenPart, Expression elsePart)
    {
        return new IfExpression(condition, thenPart, elsePart);
    }

    public static IfExpression If(Expression condition, Expression thenPart)
    {
        return If(condition, thenPart, null);
    }

    public static IfExpression If(Expression condition)
    {
        return If(condition, null);
    }

    public static ParensExpression Parens(Expression expr)
    {
        return new ParensExpression(expr);
    }

    public static ForeachExpression Foreach(VariableExpression var, Expression expr, Expression body)
    {
        return new ForeachExpression(var, expr, body);
    }

    public static ForExpression For(Expression[] init, Expression condition, Expression[] step, Expression body)
    {
        return new ForExpression(Arrays.asList(init), condition, Arrays.asList(step), body);
    }

    public static WhileExpression While(Expression condition, Expression body)
    {
        return new WhileExpression(condition, body);
    }

    public static SwitchExpression Switch(Expression selector, CaseExpression[] cases)
    {
        return new SwitchExpression(selector, Arrays.asList(cases));
    }

    public static CaseExpression Case(Expression part, Expression[] stats)
    {
        return new CaseExpression(part, Arrays.asList(stats));
    }

    public static TryExpression Try(BlockExpression body, CatchExpression[] catchers, BlockExpression finalizer, Expression[] resources)
    {
        return new TryExpression(body, Arrays.asList(catchers), finalizer, Arrays.asList(resources));
    }

    public static TryExpression Try(BlockExpression body, CatchExpression[] catchers, Expression[] resources)
    {
        return Try(body, catchers, null, resources);
    }

    public static CatchExpression Catch(VariableExpression param, BlockExpression body)
    {
        return new CatchExpression(param, body);
    }

    public static ThrowExpression Throw(Expression expr)
    {
        return new ThrowExpression(expr);
    }

    public static TypeCastExpression TypeCast(StaticClassExpression staticClass, Expression expr)
    {
        return new TypeCastExpression(staticClass.getType(), expr);
    }
}
