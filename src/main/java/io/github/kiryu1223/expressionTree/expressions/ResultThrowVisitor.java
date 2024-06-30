package io.github.kiryu1223.expressionTree.expressions;

public class ResultThrowVisitor<R> extends ResultVisitor<R>
{
    @Override
    public R visit(BinaryExpression binaryExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(UnaryExpression unaryExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ConstantExpression constantExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(FieldSelectExpression fieldSelectExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(MethodCallExpression methodCallExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ParameterExpression parameterExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(NewExpression newExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(BlockExpression blockExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(LambdaExpression<?> lambdaExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(VariableExpression variableExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(NewArrayExpression newArrayExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(IndexExpression indexExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(AssignExpression assignExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(AssignOpExpression assignOpExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ReferenceExpression referenceExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(StaticClassExpression staticClassExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ReturnExpression returnExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(BreakExpression breakExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ContinueExpression continueExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ConditionalExpression conditionalExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(IfExpression ifExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ParensExpression parensExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ForeachExpression foreachExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ForExpression forExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(WhileExpression whileExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(SwitchExpression switchExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(CaseExpression caseExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(CatchExpression catchExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(TryExpression tryExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(ThrowExpression throwExpression)
    {
        throw new RuntimeException();
    }

    @Override
    public R visit(TypeCastExpression typeCastExpression)
    {
        throw new RuntimeException();
    }
}
