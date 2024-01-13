package io.github.kiryu1223.expressionTree.dynamic;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class JavaClassFileObject extends SimpleJavaFileObject
{
    //用于存储class字节
    ByteArrayOutputStream outputStream;

    public JavaClassFileObject(String className, Kind kind) {
        super(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind);
        outputStream = new ByteArrayOutputStream();
    }


    public OutputStream openOutputStream() throws IOException
    {
        return outputStream;
    }

    public byte[] getClassBytes() {
        return outputStream.toByteArray();
    }
}