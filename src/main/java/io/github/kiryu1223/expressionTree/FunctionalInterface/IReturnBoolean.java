package io.github.kiryu1223.expressionTree.FunctionalInterface;

public interface IReturnBoolean
{
    @FunctionalInterface
    interface B1<T1>
    {
        boolean invoke(T1 t1);
    }
    @FunctionalInterface
    interface B2<T1,T2>
    {
        boolean invoke(T1 t1,T2 t2);
    }
    @FunctionalInterface
    interface B3<T1,T2,T3>
    {
        boolean invoke(T1 t1,T2 t2,T3 t3);
    }
    @FunctionalInterface
    interface B4<T1,T2,T3,T4>
    {
        boolean invoke(T1 t1,T2 t2,T3 t3,T4 t4);
    }
}
