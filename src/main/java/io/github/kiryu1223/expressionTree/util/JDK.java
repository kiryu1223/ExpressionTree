package io.github.kiryu1223.expressionTree.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class JDK
{
    private final static int Version;

    static
    {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1."))
        {
            version = version.substring(2);
        }
        Version = Integer.parseInt(version);
    }

    public static void breakSecurity()
    {
        if (JDK.is9orLater() && JDK.is15orEarlier())
        {
            try
            {
                Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger", false, Thread.currentThread().getContextClassLoader());
                Field logger = cls.getDeclaredField("logger");
                Unsafe unsafe = getUnsafe();
                unsafe.putObjectVolatile(cls, unsafe.staticFieldOffset(logger), null);
            }
            catch (NoSuchFieldException | ClassNotFoundException e)
            {
                // 把屎兜住
            }
        }
    }

    private static Unsafe getUnsafe()
    {
        try
        {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean is8()
    {
        return Version == 8;
    }

    public static boolean is9()
    {
        return Version == 9;
    }

    public static boolean is9orLater()
    {
        return Version >= 9;
    }

    public static boolean is17orLater()
    {
        return Version >= 17;
    }

    public static boolean is15orEarlier()
    {
        return Version <= 15;
    }
}
