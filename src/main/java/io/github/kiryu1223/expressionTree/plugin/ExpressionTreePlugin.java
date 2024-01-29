package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import io.github.kiryu1223.expressionTree.util.JDK;

public class ExpressionTreePlugin implements Plugin
{
    @Override
    public String getName()
    {
        return "ExpressionTreePlugin";
    }

    @Override
    public void init(JavacTask task, String... args)
    {
        JDK.breakSecurity();
        BasicJavacTask javacTask = (BasicJavacTask) task;
        ExprTreeTaskListener taskListener = new ExprTreeTaskListener(javacTask.getContext());
        javacTask.addTaskListener(taskListener);
    }
}
