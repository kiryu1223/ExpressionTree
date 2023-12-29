package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public abstract class Visitor
{
    private <T extends Expression> void deep(T expr)
    {
        if (expr != null) expr.accept(this);
    }

    private <T extends Expression> void deep(List<T> expr)
    {
        if (expr != null) for (T t : expr) t.accept(this);
    }

    public void visit(Expression expression)
    {
        throw new RuntimeException("这个方法不应该被调用 " + expression);
    }

    public void visit(BinaryExpression binaryExpression)
    {
        deep(binaryExpression.getLeft());
        deep(binaryExpression.getRight());
    }

    public void visit(UnaryExpression unaryExpression)
    {
        deep(unaryExpression.getOperand());
    }

    public void visit(ConstantExpression constantExpression)
    {

    }

    public void visit(FieldSelectExpression fieldSelectExpression)
    {
        deep(fieldSelectExpression.getExpr());
    }

    public void visit(MethodCallExpression methodCallExpression)
    {
        deep(methodCallExpression.getExpr());
        deep(methodCallExpression.getArgs());
    }

    public void visit(ParameterExpression parameterExpression)
    {

    }

    public void visit(NewExpression newExpression)
    {
        deep(newExpression.getConstructorArgs());
        deep(newExpression.getClassBody());
    }

    public void visit(BlockExpression blockExpression)
    {
        deep(blockExpression.getExpressions());
    }

    public void visit(LambdaExpression lambdaExpression)
    {
        deep(lambdaExpression.getBody());
    }

    public void visit(VariableExpression variableExpression)
    {
        deep(variableExpression.getParameter());
        deep(variableExpression.getInit());
    }

    public void visit(NewArrayExpression newArrayExpression)
    {
        deep(newArrayExpression.getCounts());
        deep(newArrayExpression.getElems());
    }

    public void visit(IndexExpression indexExpression)
    {
        deep(indexExpression.getObject());
        deep(indexExpression.getIndex());
    }

    public void visit(AssignExpression assignExpression)
    {
        deep(assignExpression.getLeft());
        deep(assignExpression.getRight());
    }

    public void visit(AssignOpExpression assignOpExpression)
    {
        deep(assignOpExpression.getLeft());
        deep(assignOpExpression.getRight());
    }
    public void visit(ReferenceExpression referenceExpression)
    {

    }
    public void visit(StaticClassExpression staticClassExpression)
    {

    }
    public void visit(ReturnExpression returnExpression)
    {
        deep(returnExpression.getExpr());
    }
}
