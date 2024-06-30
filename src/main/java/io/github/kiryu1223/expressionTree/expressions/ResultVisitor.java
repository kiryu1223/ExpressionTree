package io.github.kiryu1223.expressionTree.expressions;

public abstract class ResultVisitor<R>
{
    public R visit(Expression expression)
    {
        if (expression == null) return null;
        switch (expression.getKind())
        {
            case Binary:
                return visit((BinaryExpression) expression);
            case Unary:
                return visit((UnaryExpression) expression);
            case Constant:
                return visit((ConstantExpression) expression);
            case FieldSelect:
                return visit((FieldSelectExpression) expression);
            case MethodCall:
                return visit((MethodCallExpression) expression);
            case Parameter:
                return visit((ParameterExpression) expression);
            case New:
                return visit((NewExpression) expression);
            case NewArray:
                return visit((NewArrayExpression) expression);
            case Block:
                return visit((BlockExpression) expression);
            case Lambda:
                return visit((LambdaExpression<?>) expression);
            case Variable:
                return visit((VariableExpression) expression);
            case Index:
                return visit((IndexExpression) expression);
            case Assign:
                return visit((AssignExpression) expression);
            case AssignOp:
                return visit((AssignOpExpression) expression);
            case StaticClass:
                return visit((StaticClassExpression) expression);
            case Reference:
                return visit((ReferenceExpression) expression);
            case Return:
                return visit((ReturnExpression) expression);
            case Break:
                return visit((BreakExpression) expression);
            case Continue:
                return visit((ContinueExpression) expression);
            case Conditional:
                return visit((ConditionalExpression) expression);
            case If:
                return visit((IfExpression) expression);
            case Parens:
                return visit((ParensExpression) expression);
            case Foreach:
                return visit((ForeachExpression) expression);
            case For:
                return visit((ForExpression) expression);
            case While:
                return visit((WhileExpression) expression);
            case Switch:
                return visit((SwitchExpression) expression);
            case Case:
                return visit((CaseExpression) expression);
            case Catch:
                return visit((CatchExpression) expression);
            case Try:
                return visit((TryExpression) expression);
            case Throw:
                return visit((ThrowExpression) expression);
            case TypeCast:
                return visit((TypeCastExpression) expression);
            default:
                throw new RuntimeException();
        }
    }

    public abstract R visit(BinaryExpression binaryExpression);

    public abstract R visit(UnaryExpression unaryExpression);
    

    public abstract R visit(ConstantExpression constantExpression);
    

    public abstract R visit(FieldSelectExpression fieldSelectExpression);
    

    public abstract R visit(MethodCallExpression methodCallExpression);
    

    public abstract R visit(ParameterExpression parameterExpression);
    

    public abstract R visit(NewExpression newExpression);
    

    public abstract R visit(BlockExpression blockExpression);
    

    public abstract R visit(LambdaExpression<?> lambdaExpression);
    

    public abstract R visit(VariableExpression variableExpression);
    

    public abstract R visit(NewArrayExpression newArrayExpression);
    

    public abstract R visit(IndexExpression indexExpression);
    

    public abstract R visit(AssignExpression assignExpression);
    

    public abstract R visit(AssignOpExpression assignOpExpression);
    

    public abstract R visit(ReferenceExpression referenceExpression);
    

    public abstract R visit(StaticClassExpression staticClassExpression);
    

    public abstract R visit(ReturnExpression returnExpression);
    

    public abstract R visit(BreakExpression breakExpression);
    

    public abstract R visit(ContinueExpression continueExpression);
    

    public abstract R visit(ConditionalExpression conditionalExpression);
    

    public abstract R visit(IfExpression ifExpression);
    

    public abstract R visit(ParensExpression parensExpression);
    

    public abstract R visit(ForeachExpression foreachExpression);
    

    public abstract R visit(ForExpression forExpression);
    

    public abstract R visit(WhileExpression whileExpression);
    

    public abstract R visit(SwitchExpression switchExpression);
    

    public abstract R visit(CaseExpression caseExpression);
    

    public abstract R visit(CatchExpression catchExpression);
    

    public abstract R visit(TryExpression tryExpression);
    

    public abstract R visit(ThrowExpression throwExpression);
    

    public abstract R visit(TypeCastExpression typeCastExpression);
    
}
