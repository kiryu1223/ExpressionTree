package io.github.kiryu1223.expressionTree.delegate;

public interface Func1<T1, R> extends Delegate
{
    R invoke(T1 t1);
}
