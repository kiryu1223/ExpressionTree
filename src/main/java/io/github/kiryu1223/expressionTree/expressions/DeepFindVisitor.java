package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class DeepFindVisitor
{
    protected void deep(Expression expression)
    {
        if (expression!=null)
        {
            expression.accept(this);
        }
    }
    protected void deep(List<? extends Expression> expressions)
    {
        if (expressions!=null)
        {
            for (Expression expression : expressions)
            {
                expression.accept(this);
            }
        }
    }
    public void visit(Expression expression)
    {
        if (expression == null) return;
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
                visit((LambdaExpression<?>) expression);
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

    public void visit(LambdaExpression<?> lambdaExpression)
    {
        deep(lambdaExpression.getParameters());
        deep(lambdaExpression.getBody());
    }

    public void visit(VariableExpression variableExpression)
    {
        deep(variableExpression.getInit());
        deep(variableExpression.getParameter());
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

    public void visit(BreakExpression breakExpression)
    {
    }

    public void visit(ContinueExpression continueExpression)
    {
    }

    public void visit(ConditionalExpression conditionalExpression)
    {
        deep(conditionalExpression.getCondition());
        deep(conditionalExpression.getTruePart());
        deep(conditionalExpression.getFalsePart());
    }

    public void visit(IfExpression ifExpression)
    {
        deep(ifExpression.getCondition());
        deep(ifExpression.getThenPart());
        deep(ifExpression.getElsePart());
    }

    public void visit(ParensExpression parensExpression)
    {
        deep(parensExpression.getExpr());
    }

    public void visit(ForeachExpression foreachExpression)
    {
        deep(foreachExpression.getVar());
        deep(foreachExpression.getExpr());
        deep(foreachExpression.getBody());
    }

    public void visit(ForExpression forExpression)
    {
        deep(forExpression.getInit());
        deep(forExpression.getCondition());
        deep(forExpression.getStep());
        deep(forExpression.getBody());
    }

    public void visit(WhileExpression whileExpression)
    {
        deep(whileExpression.getCondition());
        deep(whileExpression.getBody());
    }

    public void visit(SwitchExpression switchExpression)
    {
        deep(switchExpression.getSelector());
        deep(switchExpression.getCases());
    }

    public void visit(CaseExpression caseExpression)
    {
        deep(caseExpression.getPart());
        deep(caseExpression.getStats());
    }

    public void visit(CatchExpression catchExpression)
    {
        deep(catchExpression.getParam());
        deep(catchExpression.getBody());
    }

    public void visit(TryExpression tryExpression)
    {
        deep(tryExpression.getResources());
        deep(tryExpression.getBody());
        deep(tryExpression.getCatchers());
        deep(tryExpression.getFinalizer());
    }

    public void visit(ThrowExpression throwExpression)
    {
        deep(throwExpression.getExpr());
    }

    public void visit(TypeCastExpression typeCastExpression)
    {
        deep(typeCastExpression.getExpr());
    }
}
