package io.github.kiryu1223.expressionTree.delegate;

public interface Action2<T1,T2> extends Delegate
{
    void invoke(T1 t1, T2 t2);
}
