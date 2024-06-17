package io.github.kiryu1223.expressionTree.delegate;

public interface Func2<T1, T2, R> extends Delegate
{
    R invoke(T1 t1, T2 t2);
}
