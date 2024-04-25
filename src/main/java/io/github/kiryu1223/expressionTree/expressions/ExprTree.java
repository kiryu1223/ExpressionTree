package io.github.kiryu1223.expressionTree.expressions;

import io.github.kiryu1223.expressionTree.delegate.Action0;
import io.github.kiryu1223.expressionTree.delegate.Action1;
import io.github.kiryu1223.expressionTree.delegate.Delegate;
import io.github.kiryu1223.expressionTree.util.StopWatch;

import java.util.function.Function;

public final class ExprTree<T extends Delegate>
{
    private final T delegate;
    private final LambdaExpression<T> tree;

    private ExprTree(T delegate, LambdaExpression<T> tree)
    {
        this.delegate = delegate;
        this.tree = tree;
    }

    public static <T extends Delegate> ExprTree<T> Expr(T delegate, LambdaExpression<T> tree)
    {
        return new ExprTree<>(delegate, tree);
    }

    public T getDelegate()
    {
        return delegate;
    }

    public LambdaExpression<T> getTree()
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
