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
        switch (expression.getKind())
        {
            case Binary:
                visit((BinaryExpression) expression);
                break;
            case Unary:
                visit((UnaryExpression) expression);
                break;
            case Constant:
                visit((ConstantExpression) expression);
                break;
            case FieldSelect:
                visit((FieldSelectExpression) expression);
                break;
            case MethodCall:
                visit((MethodCallExpression) expression);
                break;
            case Parameter:
                visit((ParameterExpression) expression);
                break;
            case New:
                visit((NewExpression) expression);
                break;
            case NewArray:
                visit((NewArrayExpression) expression);
                break;
            case Block:
                visit((BlockExpression) expression);
                break;
            case Lambda:
                visit((LambdaExpression) expression);
                break;
            case Variable:
                visit((VariableExpression) expression);
                break;
            case Index:
                visit((IndexExpression) expression);
                break;
            case Assign:
                visit((AssignExpression) expression);
                break;
            case AssignOp:
                visit((AssignOpExpression) expression);
                break;
            case StaticClass:
                visit((StaticClassExpression) expression);
                break;
            case Reference:
                visit((ReferenceExpression) expression);
                break;
            case Return:
                visit((ReturnExpression) expression);
                break;
            case Break:
                visit((BreakExpression) expression);
                break;
            case Continue:
                visit((ContinueExpression) expression);
                break;
            case Conditional:
                visit((ConditionalExpression) expression);
                break;
            case If:
                visit((IfExpression) expression);
                break;
            case Parens:
                visit((ParensExpression) expression);
                break;
            case Foreach:
                visit((ForeachExpression) expression);
                break;
            case For:
                visit((ForExpression) expression);
                break;
            case While:
                visit((WhileExpression) expression);
                break;
            case Switch:
                visit((SwitchExpression) expression);
                break;
            case Case:
                visit((CaseExpression) expression);
                break;
            case Catch:
                visit((CatchExpression) expression);
                break;
            case Try:
                visit((TryExpression) expression);
                break;
            case Throw:
                visit((ThrowExpression) expression);
                break;
            case TypeCast:
                visit((TypeCastExpression) expression);
                break;
        }
    }
    public void visit(BinaryExpression binaryExpression) {}
    public void visit(UnaryExpression unaryExpression) {}
    public void visit(ConstantExpression constantExpression) {}
    public void visit(FieldSelectExpression fieldSelectExpression) {}
    public void visit(MethodCallExpression methodCallExpression) {}
    public void visit(ParameterExpression parameterExpression) {}
    public void visit(NewExpression newExpression) {}
    public void visit(BlockExpression blockExpression) {}
    public void visit(LambdaExpression lambdaExpression) {}
    public void visit(VariableExpression variableExpression) {}
    public void visit(NewArrayExpression newArrayExpression) {}
    public void visit(IndexExpression indexExpression) {}
    public void visit(AssignExpression assignExpression) {}
    public void visit(AssignOpExpression assignOpExpression) {}
    public void visit(ReferenceExpression referenceExpression) {}
    public void visit(StaticClassExpression staticClassExpression) {}
    public void visit(ReturnExpression returnExpression) {}
    public void visit(BreakExpression breakExpression) {}
    public void visit(ContinueExpression continueExpression) {}
    public void visit(ConditionalExpression conditionalExpression) {}
    public void visit(IfExpression ifExpression) {}
    public void visit(ParensExpression parensExpression) {}
    public void visit(ForeachExpression foreachExpression) {}
    public void visit(ForExpression forExpression) {}
    public void visit(WhileExpression whileExpression) {}
    public void visit(SwitchExpression switchExpression) {}
    public void visit(CaseExpression caseExpression) {}
    public void visit(CatchExpression catchExpression) {}
    public void visit(TryExpression tryExpression) {}
    public void visit(ThrowExpression throwExpression) {}
    public void visit(TypeCastExpression typeCastExpression) {}
}
