package io.github.kiryu1223.expressionTree.dynamic;

import io.github.kiryu1223.expressionTree.expressions.*;
import io.github.kiryu1223.expressionTree.plugin.ImportInfo;
import net.openhft.compiler.CompilerUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//@Deprecated
public class DynamicCompilerUtil
{
    private static final String call = "call";
    private static final String Dynamic_ = "Dynamic_";
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final ConcurrentHashMap<String, DynamicMethod> DynamicMethodCache = new ConcurrentHashMap<>();

    public static DynamicMethod dynamicCompile(LambdaExpression lambdaExpression)
    {
//        try
//        {
//            List<Class<?>> types = new ArrayList<>();
//            List<Object> defValues = new ArrayList<>();
//            String classNane = Dynamic_ + UUID.randomUUID().toString().replace("-", "");
//            String code = writeCode(classNane, lambdaExpression, defValues, types);
//            DynamicCompiler DYNAMIC_COMPILER = new DynamicCompiler(classLoader);
//            Class<?> compiled = DYNAMIC_COMPILER.compile(classNane, code);
//            if (compiled == null) return null;
//            Method method = compiled.getMethod(call, types.toArray(new Class<?>[0]));
//            return new DynamicMethod(method, defValues);
//        }
//        catch (NoSuchMethodException | ClassNotFoundException e)
//        {
//            throw new RuntimeException(e);
//        }
//        List<Class<?>> types = new ArrayList<>();
//        List<Object> defValues = new ArrayList<>();
//        String classNane = Dynamic_ + UUID.randomUUID().toString().replace("-", "");
//        String code = writeCode(classNane, lambdaExpression, defValues, types);
//        System.out.println(code);
//        try
//        {
//            //生成源代码的JavaFileObject
//            SimpleJavaFileObject fileObject = new JavaSourceFromString(classNane, code);
//            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//            //被修改后的JavaFileManager
//            JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
//            //执行编译
//            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Collections.singletonList(fileObject));
//            task.call();
//            //获得ClassLoader，加载class文件
//            ClassLoader classLoader = fileManager.getClassLoader(null);
//            Class<?> type = classLoader.loadClass(classNane);
//            Method method = type.getMethod(call, types.toArray(new Class<?>[0]));
//            return new DynamicMethod(method, defValues);
//        }
//        catch (Exception e)
//        {
//            throw new RuntimeException(e);
//        }
        List<Class<?>> types = new ArrayList<>();
        List<Object> defValues = new ArrayList<>();
        String className = Dynamic_ + UUID.randomUUID().toString().replace("-", "");
        String code = writeCode(className, lambdaExpression, defValues, types);
        DynamicMethod dynamicMethod = DynamicMethodCache.get(code);
        if (dynamicMethod != null)
        {
            return dynamicMethod;
        }
        try
        {
            Class<?> clazz = CompilerUtils.CACHED_COMPILER.loadFromJava(className, code);
            //Method method = clazz.getMethod(call, types.toArray(new Class<?>[0]));
            MethodType methodType = MethodType.methodType(lambdaExpression.getReturnType(), types);
            MethodHandle methodHandle = lookup.findStatic(clazz, call, methodType);
            dynamicMethod = new DynamicMethod(methodHandle, defValues);
            DynamicMethodCache.put(code, dynamicMethod);
            return dynamicMethod;
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String writeCode(String classNane, LambdaExpression lambdaExpression, List<Object> defValues, List<Class<?>> types)
    {
        List<ParameterExpression> parameters = lambdaExpression.getParameters();
        Expression body = lambdaExpression.getBody();
        Class<?> returnType = getRawClass(lambdaExpression.getReturnType());
        StringBuilder code = new StringBuilder();
        FindImport findImport = new FindImport();
        lambdaExpression.accept(findImport);
        List<ImportInfo> importInfos = findImport.getImportInfos();
        if (!returnType.isPrimitive() && returnType.getCanonicalName() != null)
        {
            ImportInfo importInfo = new ImportInfo(returnType.getCanonicalName(), false);
            if (!importInfos.contains(importInfo))
            {
                importInfos.add(importInfo);
            }
        }
        for (ImportInfo importInfo : importInfos)
        {
            code.append("import ").append(importInfo.getName()).append(";\n");
        }
        code.append("\n");

        code.append("public class ").append(classNane).append(" {\n");
        //入参参数
        code.append("public static ");
        code.append(returnType.getSimpleName()).append(" ").append(call).append(" (");
        for (ParameterExpression parameter : parameters)
        {
            code.append(parameter.getType().getSimpleName()).append(" ")
                    .append(parameter.getName()).append(",");
            types.add(parameter.getType());
        }
        //引用参数
        body.accept(new DeepFindVisitor()
        {
            @Override
            public void visit(ReferenceExpression ref)
            {
                Class<?> type = ref.getRef().getClass();
                code.append(type.getSimpleName()).append(" ")
                        .append(ref.getName()).append(",");
                defValues.add(ref.getRef());
                types.add(type);
            }
        });
        if (code.charAt(code.length() - 1) == ',') code.deleteCharAt(code.length() - 1);
        code.append(")\n");
        //方法体
        if (body.getKind() != Kind.Block
                && lambdaExpression.getReturnType() != void.class)
        {
            code.append("{\n").append("return ").append(body).append(";").append("\n}");
        }
        else
        {
            code.append(body);
        }
        code.append("\n}");
        return code.toString();
    }

    private static Class<?> getRawClass(Class<?> clazz)
    {
        return clazz.isAnonymousClass() ? getRawClass(clazz.getSuperclass()) : clazz;
    }

    private static class FindImport extends DeepFindVisitor
    {
        private final List<ImportInfo> importInfos = new ArrayList<>();

        private List<ImportInfo> getImportInfos()
        {
            return importInfos;
        }

        private void addInfo(Class<?> type)
        {
            if (type.isPrimitive()) return;
            ImportInfo importInfo = new ImportInfo(type.getCanonicalName(), false);
            if (!importInfos.contains(importInfo))
            {
                importInfos.add(importInfo);
            }
        }

        @Override
        public void visit(StaticClassExpression staticClass)
        {
            Class<?> type = staticClass.getType();
            addInfo(type);
        }

        @Override
        public void visit(ParameterExpression parameter)
        {
            Class<?> type = parameter.getType();
            addInfo(type);
        }

        @Override
        public void visit(ReferenceExpression reference)
        {
            Class<?> type = reference.getRef().getClass();
            addInfo(type);
        }

        @Override
        public void visit(NewExpression newExpression)
        {
            Class<?> type = newExpression.getType();
            addInfo(type);
            super.visit(newExpression);
        }
    }
}
