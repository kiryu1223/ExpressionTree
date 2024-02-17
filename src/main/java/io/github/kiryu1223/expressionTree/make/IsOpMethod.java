package io.github.kiryu1223.expressionTree.make;

import io.github.kiryu1223.expressionTree.expressions.OperatorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IsOpMethod
{
    OperatorType value();
}
