package io.github.kiryu1223.expressionTree.util;

public class StopWatch
{
    private static long time = 0;

    public static void start()
    {
        time = System.currentTimeMillis();
    }

    public static void peek()
    {
        System.out.println(System.currentTimeMillis() - time);
    }

    public static void stop()
    {
        peek();
        time = 0;
    }
}
