package io.github.kiryu1223.expressionTree.delegate;

public interface Action3<T1, T2, T3> extends Delegate
{
    void invoke(T1 t1, T2 t2, T3 t3);
}
