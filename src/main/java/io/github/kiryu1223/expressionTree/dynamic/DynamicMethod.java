package io.github.kiryu1223.expressionTree.dynamic;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicMethod
{
    private final MethodHandle methodHandle;
    private final List<Object> defValues;

    public DynamicMethod(MethodHandle methodHandle, List<Object> defValues)
    {
        this.methodHandle = methodHandle;
        this.defValues = defValues;
    }

    public <T> T invoke(Object... values)
    {
        try
        {
            Object[] objects = assembly(values);
            switch (objects.length)
            {
                case 0:
                    return (T) methodHandle.invoke();
                case 1:
                    return (T) methodHandle.invoke(objects[0]);
                case 2:
                    return (T) methodHandle.invoke(objects[0],objects[1]);
                case 3:
                    return (T) methodHandle.invoke(objects[0],objects[1],objects[2]);
                case 4:
                    return (T) methodHandle.invoke(objects[0],objects[1],objects[2],objects[3]);
                default:
                    return (T) methodHandle.invokeWithArguments(objects);
            }
//            if (objects.length == 0)
//            {
//                return (T) methodHandle.invoke();
//            }
//            else
//            {
//                return (T) methodHandle.invokeWithArguments(objects);
//            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    private Object[] assembly(Object... values)
    {
        Object[] objects = new Object[values.length + defValues.size()];
        int i = 0;
        for (Object value : values)
        {
            objects[i++] = value;
        }
        for (Object defValue : defValues)
        {
            objects[i++] = defValue;
        }
        return objects;
    }
}
