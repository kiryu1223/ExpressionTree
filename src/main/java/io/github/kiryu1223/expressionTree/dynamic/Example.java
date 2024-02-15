package io.github.kiryu1223.expressionTree.dynamic;

import io.github.kiryu1223.expressionTree.delegate.Action0;
import io.github.kiryu1223.expressionTree.delegate.Action1;
import io.github.kiryu1223.expressionTree.delegate.Func0;
import io.github.kiryu1223.expressionTree.delegate.Func1;
import io.github.kiryu1223.expressionTree.expressions.*;

import static io.github.kiryu1223.expressionTree.expressions.ExprTree.Expr;

public class Example
{
    public static void main(String[] args) throws Exception
    {
//        long start = System.currentTimeMillis();
//        System.out.println("动态编译开始");
//        ParameterExpression left = Expression.Parameter(int.class, "a");
//        ConstantExpression right = Expression.Constant(1);
//        BinaryExpression binary = Expression.Binary(left, right, OperatorType.EQ);
//        LambdaExpression<Func1<Integer, Boolean>> lambda = Expression.<Func1<Integer, Boolean>>Lambda(binary, new ParameterExpression[]{left}, boolean.class);
//        Func1<Integer, Boolean> compile = lambda.compile();
//        boolean invoke = compile.invoke(100);
//        System.out.println(invoke);
//        System.out.println("动态编译结束，耗时:" + (System.currentTimeMillis() - start) + "ms");

        test(s -> System.out.println(s));

//        Action0 a2 = () -> {};
//        ExprTree<Action0> a1 = () -> {};
    }

    static void test(@Expr Action1<String> action)
    {
        throw new RuntimeException();
    }

    static void test(ExprTree<Action1<String>> action)
    {
        LambdaExpression<Action1<String>> tree = action.getTree();
        tree.compile().invoke("^^");
    }

    String sss;
    public Action0 act = () ->
    {
        System.out.println(sss);
    };

    public Example(String ss)
    {
        sss = ss;
    }
}
