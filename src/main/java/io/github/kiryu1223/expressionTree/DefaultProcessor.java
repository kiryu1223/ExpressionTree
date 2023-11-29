package io.github.kiryu1223.expressionTree;

import io.github.kiryu1223.expressionTree.FunctionalInterface.ExpressionTree;
import io.github.kiryu1223.expressionTree.FunctionalInterface.IReturnGeneric;
import io.github.kiryu1223.expressionTree.info.ClassInfo;
import io.github.kiryu1223.expressionTree.info.ImportInfo;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.List;

public class DefaultProcessor extends AbstractExpressionProcessor
{
    @Override
    public void registerManager(List<Class<?>> classList)
    {
        //System.out.println("DefaultProcessor 被调用");
    }
}
