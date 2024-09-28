package io.github.kiryu1223.expressionTree.ext;

import com.sun.source.util.TaskEvent;

public interface IExtensionService
{
    void started(TaskEvent event) throws Throwable;

    void finished(TaskEvent event) throws Throwable;
}
