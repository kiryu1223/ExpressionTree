package io.github.kiryu1223.expressionTree.dynamic;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;

public class ClassFileManager extends ForwardingJavaFileManager
{

    private JavaClassFileObject classFileObject;
    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     */
    protected ClassFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    /**
     * Gets a JavaFileObject file object for output
     * representing the specified class of the specified kind in the given location.
     */

    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                                               FileObject sibling) throws IOException
    {
        classFileObject = new JavaClassFileObject(className, kind);
        return classFileObject;
    }


    //获得一个定制ClassLoader，返回我们保存在内存的类
    public ClassLoader getClassLoader(Location location) {
        return new ClassLoader() {

            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] classBytes = classFileObject.getClassBytes();
                return super.defineClass(name, classBytes, 0, classBytes.length);
            }
        };
    }
}
