package io.github.kiryu1223.expressionTree.ext;

import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.util.Context;

public interface IExtensionService
{
    void init(Context context);

    void started(TaskEvent event) throws Throwable;

    void finished(TaskEvent event) throws Throwable;
}
