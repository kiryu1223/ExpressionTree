package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import io.github.kiryu1223.expressionTree.plugin.ExprTreeTaskListener;


public class ExpressionTreePlugin implements Plugin
{
    public ExpressionTreePlugin() {}

    @Override
    public String getName()
    {
        return "ExpressionTree";
    }

    @Override
    public void init(JavacTask task, String... args)
    {
        BasicJavacTask javacTask = (BasicJavacTask) task;
        ExprTreeTaskListener taskListener = new ExprTreeTaskListener(javacTask.getContext());
        javacTask.addTaskListener(taskListener);
    }
}
