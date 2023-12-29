package io.github.kiryu1223.expressionTree.expressions;

public class UnaryExpression extends Expression
{
    private final Expression operand;
    private final OperatorType operatorType;

    public UnaryExpression(Expression operand, OperatorType operatorType)
    {
        this.operand = operand;
        this.operatorType = operatorType;
    }

    public Expression getOperand()
    {
        return operand;
    }

    public OperatorType getOperatorType()
    {
        return operatorType;
    }

    @Override
    public Kind getKind()
    {
        return Kind.Unary;
    }

    @Override
    public String toString()
    {
        switch (operatorType)
        {
            case POSTINC:
            case POSTDEC:
                return operand + operatorType.getOperator();
            default:
                return operatorType.getOperator() + operand;
        }
    }
}
