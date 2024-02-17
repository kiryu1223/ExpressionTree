package io.github.kiryu1223.expressionTree.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReflectUtil
{
    private final static Map<Class<?>, Map<String, Field>> fieldCache = new ConcurrentHashMap<>();
    private final static Map<Class<?>, Map<String, Map<Class<?>[], Method>>> methodCache = new ConcurrentHashMap<>();

    public static Field getField(Class<?> clazz, String name)
    {
        try
        {
            if (!fieldCache.containsKey(clazz))
            {
                fieldCache.put(clazz, new ConcurrentHashMap<>());
            }
            Map<String, Field> fieldMap = fieldCache.get(clazz);
            if (!fieldMap.containsKey(name))
            {
                Field declaredField = clazz.getDeclaredField(name);
                fieldMap.put(name, declaredField);
            }
            return fieldMap.get(name);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>[] classes)
    {
        try
        {
            Map<Class<?>[], Method> methodMap = getMethodMap(clazz, name);
            if (!methodMap.containsKey(classes))
            {
                Method declaredMethod = clazz.getDeclaredMethod(name, classes);
                methodMap.put(classes, declaredMethod);
            }
            return methodMap.get(classes);
        }
        catch (NoSuchMethodException e)
        {
            for (Method declaredMethod : clazz.getDeclaredMethods())
            {
                if (!declaredMethod.getName().equals(name)) continue;
                if (declaredMethod.getParameterCount() != classes.length) continue;
                Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                if (AllAssignableFrom(parameterTypes, classes))
                {
                    Map<Class<?>[], Method> methodMap = getMethodMap(clazz, name);
                    methodMap.put(classes, declaredMethod);
                    return declaredMethod;
                }
            }
            throw new RuntimeException(e);
        }
    }

    private static Map<Class<?>[], Method> getMethodMap(Class<?> clazz, String name)
    {
        if (!methodCache.containsKey(clazz))
        {
            methodCache.put(clazz, new ConcurrentHashMap<>());
        }
        Map<String, Map<Class<?>[], Method>> nameMethodMap = methodCache.get(clazz);
        if (!nameMethodMap.containsKey(name))
        {
            nameMethodMap.put(name, new ConcurrentHashMap<>());
        }
        return nameMethodMap.get(name);
    }

    private static boolean AllAssignableFrom(Class<?>[] parameterTypes, Class<?>[] classes)
    {
        for (int i = 0; i < parameterTypes.length; i++)
        {
            if (parameterTypes[i].isPrimitive() || classes[i].isPrimitive())
            {
                if (!specialJudgments(parameterTypes[i], classes[i]))
                {
                    return false;
                }
            }
            else if (!parameterTypes[i].isAssignableFrom(classes[i]))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean specialJudgments(Class<?> classA, Class<?> classB)
    {
        if (classA == classB) return true;
        else if (classA == Long.TYPE || classA == Long.class)
        {
            if (classB != Long.TYPE && classB != Long.class
                    && classB != Integer.TYPE && classB != Integer.class
                    && classB != Short.TYPE && classB != Short.class
                    && classB != Byte.TYPE && classB != Byte.class)
            {
                return false;
            }
        }
        else if (classA == Integer.TYPE || classA == Integer.class)
        {
            if (classB != Integer.TYPE && classB != Integer.class
                    && classB != Short.TYPE && classB != Short.class
                    && classB != Byte.TYPE && classB != Byte.class)
            {
                return false;
            }
        }
        else if (classA == Short.TYPE || classA == Short.class)
        {
            if (classB != Short.TYPE && classB != Short.class
                    && classB != Byte.TYPE && classB != Byte.class)
            {
                return false;
            }
        }
        else if (classA == Byte.TYPE || classA == Byte.class)
        {
            if (classB != Byte.TYPE && classB != Byte.class)
            {
                return false;
            }
        }
        else if (classA == Character.TYPE || classA == Character.class)
        {
            if (classB != Character.TYPE && classB != Character.class)
            {
                return false;
            }
        }
        else if (classA == Boolean.TYPE || classA == Boolean.class)
        {
            if (classB != Boolean.TYPE && classB != Boolean.class)
            {
                return false;
            }
        }
        else if (classA == Double.TYPE || classA == Double.class)
        {
            if (classB != Long.TYPE && classB != Long.class
                    && classB != Integer.TYPE && classB != Integer.class
                    && classB != Short.TYPE && classB != Short.class
                    && classB != Byte.TYPE && classB != Byte.class
                    && classB != Float.TYPE && classB != Float.class
                    && classB != Double.TYPE && classB != Double.class)
            {
                return false;
            }
        }
        else if (classA == Float.TYPE || classA == Float.class)
        {
            if (classB != Long.TYPE && classB != Long.class
                    && classB != Integer.TYPE && classB != Integer.class
                    && classB != Short.TYPE && classB != Short.class
                    && classB != Byte.TYPE && classB != Byte.class
                    && classB != Float.TYPE && classB != Float.class)
            {
                return false;
            }
        }
        return true;
    }

    public static <T> T getFieldValue(Object o, String name)
    {
        try
        {
            Field field = getField(o.getClass(), name);
            field.setAccessible(true);
            return (T) field.get(o);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invokeMethod(Object o, String name, List<Object> values)
    {
        try
        {
            List<Class<?>> classes = values.stream().map(a -> a.getClass()).collect(Collectors.toList());
            Method method = getMethod(o.getClass(), name, classes.toArray(new Class[0]));
            method.setAccessible(true);
            return (T) method.invoke(o, values.toArray());
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }
}
