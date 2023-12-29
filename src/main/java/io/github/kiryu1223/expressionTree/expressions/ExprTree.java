package io.github.kiryu1223.expressionTree.expressions;

import io.github.kiryu1223.expressionTree.delegate.Delegate;

public final class ExprTree<T extends Delegate>
{
    private final T delegate;
    private LambdaExpression tree;

    public ExprTree(T delegate)
    {
        this.delegate = delegate;
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
