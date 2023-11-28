package io.github.kiryu1223.expressionTree.FunctionalInterface;

public interface IReturnGeneric
{
    @FunctionalInterface
    interface G1<T1,R>
    {
        R invoke(T1 t1);
    }
    @FunctionalInterface
    interface G2<T1,T2,R>
    {
        R invoke(T1 t1,T2 t2);
    }
    @FunctionalInterface
    interface G3<T1,T2,T3,R>
    {
        R invoke(T1 t1,T2 t2,T3 t3);
    }
    @FunctionalInterface
    interface G4<T1,T2,T3,T4,R>
    {
        R invoke(T1 t1,T2 t2,T3 t3,T4 t4);
    }
}
