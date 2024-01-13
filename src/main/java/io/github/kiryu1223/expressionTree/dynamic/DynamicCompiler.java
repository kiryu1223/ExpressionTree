package io.github.kiryu1223.expressionTree.dynamic;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DynamicCompiler
{
    private final DynamicClassLoader classLoader;

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public DynamicCompiler(ClassLoader classLoader)
    {
        this.classLoader = new DynamicClassLoader(classLoader);
    }

    public Class<?> compile(String className, String src) throws ClassNotFoundException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<javax.tools.JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        JavaFileManager javaFileManager = new JavaFileManager(standardFileManager);
        List<JavaFileObject> files = Collections.singletonList(new JavaFileObject(className, src));
        List<String> options = Arrays.asList("-encoding", StandardCharsets.UTF_8.toString());
        JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null, files);
        if (task.call())
        {
            return javaFileManager.toClass(classLoader);
        }
        else
        {
            System.out.println("Compilation failed.");
            for (Diagnostic<? extends javax.tools.JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics())
            {
                System.out.println(diagnostic.getMessage(null));
            }
            return null;
        }
    }

    private static class JavaFileObject extends SimpleJavaFileObject
    {

        private final CharSequence content;

        protected JavaFileObject(String className, CharSequence content)
        {
            super(URI.create("string:///" + className.replace('.', '/')
                    + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException
        {
            return content;
        }
    }

    private static class JavaFileManager extends ForwardingJavaFileManager<javax.tools.JavaFileManager>
    {

        private JavaClassObject javaFileObject;

        protected JavaFileManager(javax.tools.JavaFileManager fileManager)
        {
            super(fileManager);
        }

        @Override
        public javax.tools.JavaFileObject getJavaFileForOutput(Location location, String className, javax.tools.JavaFileObject.Kind kind, FileObject sibling) throws IOException
        {
            javaFileObject = new JavaClassObject(className, kind);
            return javaFileObject;
        }

        public Class<?> toClass(DynamicClassLoader classLoader)
        {
            return javaFileObject.toClass(classLoader);
        }
    }

    private static class JavaClassObject extends SimpleJavaFileObject
    {
        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final String name;

        protected JavaClassObject(String name, Kind kind)
        {
            super(URI.create("string:///" + name.replace('.', '/')
                    + kind.extension), kind);
            this.name = name;
        }

        @Override
        public OutputStream openOutputStream() throws IOException
        {
            return bos;
        }

        Class<?> toClass(DynamicClassLoader dynamicClassLoader)
        {
            return dynamicClassLoader.defineClass(name, bos.toByteArray());
        }
    }

    private static class DynamicClassLoader extends URLClassLoader
    {

        public DynamicClassLoader(ClassLoader parent)
        {
            super(new URL[0], parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException
        {
            return super.findClass(name);
        }

        public Class<?> defineClass(String name, byte[] bytes)
        {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
