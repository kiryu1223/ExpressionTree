package io.github.kiryu1223.expressionTree.dynamic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicMethod
{
    private final Method method;
    private final List<Object> defValues;

    public DynamicMethod(Method method, List<Object> defValues)
    {
        this.method = method;
        this.defValues = defValues;
    }

    public <T> T invoke(Object... values)
    {
        try
        {
            if (!defValues.isEmpty())
            {
                List<Object> list = new ArrayList<>(Arrays.asList(values));
                list.addAll(defValues);
                return (T) method.invoke(null, list.toArray());
            }
            else
            {
                return (T) method.invoke(null, values);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }
}
