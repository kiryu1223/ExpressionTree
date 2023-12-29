package io.github.kiryu1223.expressionTree.delegate;

public interface Action4<T1, T2, T3, T4> extends Delegate
{
    void invoke(T1 t1, T2 t2, T3 t3, T4 t4);
}
