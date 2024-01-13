package io.github.kiryu1223.expressionTree.expressions;

public class VariableExpression extends Expression
{
    private final ParameterExpression parameter;
    private final Expression init;

    public VariableExpression(ParameterExpression parameter, Expression init)
    {
        this.parameter = parameter;
        this.init = init;
    }

    public ParameterExpression getParameter()
    {
        return parameter;
    }

    public Expression getInit()
    {
        return init;
    }

    public String getName()
    {
        return parameter.getName();
    }

    public Class<?> getType()
    {
        return parameter.getType();
    }

    @Override
    public Kind getKind()
    {
        return Kind.Variable;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(parameter.getType().getSimpleName()).append(" ")
                .append(parameter.getName());
        if (init != null)
        {
            sb.append(" = ").append(init);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VariableExpression that = (VariableExpression) obj;
        return parameter.equals(that.parameter) && init.equals(that.init);
    }
}
