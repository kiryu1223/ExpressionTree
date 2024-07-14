package io.github.kiryu1223.expressionTree.expressions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Expr
{
    BodyType value() default BodyType.Any;

    enum BodyType
    {
        Any,
        Expr,
        Block,
    }
}
