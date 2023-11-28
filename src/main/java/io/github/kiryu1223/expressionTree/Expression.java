package io.github.kiryu1223.expressionTree;

import io.github.kiryu1223.expressionTree.expressionV2.IExpression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注了@Expression的lambda形参将会在编译期被寻找,
 * 其关键特征会被保存在编译流程中使用，并且被替换成表达式树
 * <br><br>
 * @author kiryu1223
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Expression
{
    Class<? extends IExpression> value() default IExpression.class;
}
