package io.github.kiryu1223.expressionTree.expressions;

public abstract class GenericsVisitor<V>
{
    public void visit(Expression expression, V v) {
        if (expression == null) return;
        switch (expression.getKind())
        {
            case Binary:
                visit((BinaryExpression) expression, v);
                break;
            case Unary:
                visit((UnaryExpression) expression, v);
                break;
            case Constant:
                visit((ConstantExpression) expression, v);
                break;
            case FieldSelect:
                visit((FieldSelectExpression) expression, v);
                break;
            case MethodCall:
                visit((MethodCallExpression) expression, v);
                break;
            case Parameter:
                visit((ParameterExpression) expression, v);
                break;
            case New:
                visit((NewExpression) expression, v);
                break;
            case NewArray:
                visit((NewArrayExpression) expression, v);
                break;
            case Block:
                visit((BlockExpression) expression, v);
                break;
            case Lambda:
                visit((LambdaExpression) expression, v);
                break;
            case Variable:
                visit((VariableExpression) expression, v);
                break;
            case Index:
                visit((IndexExpression) expression, v);
                break;
            case Assign:
                visit((AssignExpression) expression, v);
                break;
            case AssignOp:
                visit((AssignOpExpression) expression, v);
                break;
            case StaticClass:
                visit((StaticClassExpression) expression, v);
                break;
            case Reference:
                visit((ReferenceExpression) expression, v);
                break;
            case Return:
                visit((ReturnExpression) expression, v);
                break;
            case Break:
                visit((BreakExpression) expression, v);
                break;
            case Continue:
                visit((ContinueExpression) expression, v);
                break;
            case Conditional:
                visit((ConditionalExpression) expression, v);
                break;
            case If:
                visit((IfExpression) expression, v);
                break;
            case Parens:
                visit((ParensExpression) expression, v);
                break;
            case Foreach:
                visit((ForeachExpression) expression, v);
                break;
            case For:
                visit((ForExpression) expression, v);
                break;
            case While:
                visit((WhileExpression) expression, v);
                break;
            case Switch:
                visit((SwitchExpression) expression, v);
                break;
            case Case:
                visit((CaseExpression) expression, v);
                break;
            case Catch:
                visit((CatchExpression) expression, v);
                break;
            case Try:
                visit((TryExpression) expression, v);
                break;
            case Throw:
                visit((ThrowExpression) expression, v);
                break;
            case TypeCast:
                visit((TypeCastExpression) expression, v);
                break;
        }
    }

    public void visit(BinaryExpression binaryExpression, V v) {}

    public void visit(UnaryExpression unaryExpression, V v) {}

    public void visit(ConstantExpression constantExpression, V v) {}

    public void visit(FieldSelectExpression fieldSelectExpression, V v) {}

    public void visit(MethodCallExpression methodCallExpression, V v) {}

    public void visit(ParameterExpression parameterExpression, V v) {}

    public void visit(NewExpression newExpression, V v) {}

    public void visit(BlockExpression blockExpression, V v) {}

    public void visit(LambdaExpression lambdaExpression, V v) {}

    public void visit(VariableExpression variableExpression, V v) {}

    public void visit(NewArrayExpression newArrayExpression, V v) {}

    public void visit(IndexExpression indexExpression, V v) {}

    public void visit(AssignExpression assignExpression, V v) {}

    public void visit(AssignOpExpression assignOpExpression, V v) {}

    public void visit(ReferenceExpression referenceExpression, V v) {}
    public void visit(StaticClassExpression staticClassExpression, V v) {}

    public void visit(ReturnExpression returnExpression, V v) {}

    public void visit(BreakExpression breakExpression, V v) {}

    public void visit(ContinueExpression continueExpression, V v) {}

    public void visit(ConditionalExpression conditionalExpression, V v) {}

    public void visit(IfExpression ifExpression, V v) {}

    public void visit(ParensExpression parensExpression, V v) {}

    public void visit(ForeachExpression foreachExpression, V v) {}

    public void visit(ForExpression forExpression, V v) {}

    public void visit(WhileExpression whileExpression, V v) {}

    public void visit(SwitchExpression switchExpression, V v) {}

    public void visit(CaseExpression caseExpression, V v) {}

    public void visit(CatchExpression catchExpression, V v) {}

    public void visit(TryExpression tryExpression, V v) {}

    public void visit(ThrowExpression throwExpression, V v) {}

    public void visit(TypeCastExpression typeCastExpression, V v) {}
}
