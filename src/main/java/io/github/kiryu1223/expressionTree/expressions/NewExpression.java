package io.github.kiryu1223.expressionTree.expressions;

import java.util.List;

public class NewExpression extends Expression
{
    private final Class<?> type;
    private final List<Class<?>> typeArgs;
    private final List<Expression> constructorArgs;
    private final BlockExpression classBody;

    public NewExpression(Class<?> type, List<Class<?>> typeArgs, List<Expression> constructorArgs, BlockExpression classBody)
    {
        this.type = type;
        this.typeArgs = typeArgs;
        this.constructorArgs = constructorArgs;
        this.classBody = classBody;
    }

    public Class<?> getType()
    {
        return type;
    }

    public List<Class<?>> getTypeArgs()
    {
        return typeArgs;
    }

    public List<Expression> getConstructorArgs()
    {
        return constructorArgs;
    }

    public BlockExpression getClassBody()
    {
        return classBody;
    }

    @Override
    public Kind getKind()
    {
        return Kind.New;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("new ")
                .append(type.isAnonymousClass() ?
                        type.getSuperclass().getSimpleName() :
                        type.getSimpleName())
                .append("(");
        for (Expression constructorArg : constructorArgs)
        {
            sb.append(constructorArg).append(",");
        }
        if (sb.charAt(sb.length() - 1) == ',')
        {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");
        if (classBody != null)
        {
            sb.append(classBody);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NewExpression that = (NewExpression) obj;
        return type.equals(that.type) && constructorArgs.equals(that.constructorArgs)
                && classBody.equals(that.classBody);
    }
}
