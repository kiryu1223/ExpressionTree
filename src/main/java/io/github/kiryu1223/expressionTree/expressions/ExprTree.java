package io.github.kiryu1223.expressionTree.expressions;

import io.github.kiryu1223.expressionTree.delegate.Action0;
import io.github.kiryu1223.expressionTree.delegate.Action1;
import io.github.kiryu1223.expressionTree.delegate.Delegate;

import java.util.function.Function;

public final class ExprTree<T extends Delegate>
{
    private final T delegate;
    private LambdaExpression tree;

    public ExprTree(T delegate)
    {
        this.delegate = delegate;
    }

    public static <T extends Delegate> ExprTree<T> ExprTree(T delegate)
    {
        return new ExprTree<T>(delegate);
    }

    public static <T extends Delegate> ExprTree<T> ExprTree(T delegate, LambdaExpression tree)
    {
        return new ExprTree<T>(delegate, tree);
    }

    public static <T extends Delegate> ExprTree<T> Expr(T delegate)
    {
        return new ExprTree<T>(delegate);
    }

    public static <T extends Delegate> ExprTree<T> Expr(T delegate, LambdaExpression tree)
    {
        return new ExprTree<T>(delegate, tree);
    }

    public ExprTree(T delegate, LambdaExpression tree)
    {
        this.delegate = delegate;
        this.tree = tree;
    }

    public T getDelegate()
    {
        return delegate;
    }

    public LambdaExpression getTree()
    {
        return tree;
    }

    @Override
    public String toString()
    {
        return "ExprTree{" +
                "delegate=" + delegate +
                ", tree=" + tree +
                '}';
    }
}
