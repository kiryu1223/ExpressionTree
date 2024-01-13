package io.github.kiryu1223.expressionTree.dynamic;

import io.github.kiryu1223.expressionTree.expressions.*;

import java.lang.String;

public class DynamicCompilationExample
{
    // 内存直接动态编译
    public static void main(String[] args) throws Exception
    {
        long start = System.currentTimeMillis();
        System.out.println("动态编译开始");
        ConstantExpression left = Expression.Constant(1);
        ConstantExpression right = Expression.Constant(1);
        BinaryExpression binary = Expression.Binary(left, right, OperatorType.EQ);
        LambdaExpression lambda = Expression.Lambda(binary, new ParameterExpression[0], boolean.class);
        DynamicMethod compiler = lambda.compile();
        System.out.println(compiler.<Boolean>invoke());
        System.out.println("动态编译结束，耗时:" + (System.currentTimeMillis() - start) + "ms");
    }
}
