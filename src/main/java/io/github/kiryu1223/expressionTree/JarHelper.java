package io.github.kiryu1223.expressionTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarHelper
{
    private static final byte[] JAR_MAGIC = {'P', 'K', 3, 4};
    private final ClassLoader classLoader;
    private final Class<?> clazz;

    JarHelper(Class<?> clazz)
    {
        this.clazz = clazz;
        this.classLoader = clazz.getClassLoader();
    }

    List<Class<?>> getClasses()
    {
        try
        {
            String packageName = clazz.getPackage().getName();
            List<String> list = listResource(packageName);
            List<Class<?>> result = new ArrayList<>();
            if (list != null)
            {
                for (String name : list)
                {
                    name = name.substring(0, name.lastIndexOf(".")).replace("/", ".");
                    try
                    {
                        Class<?> aClass = classLoader.loadClass(name);
                        result.add(aClass);
                    }
                    catch (ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private List<String> listResource(String packageName) throws IOException
    {
        String path = packageName.replace(".", "/");
        URL url = classLoader.getResource(path);
        if (url != null)
        {
            URL jarUrl = findJarForResource(url);
            // 如果是jar包的话
            if (jarUrl != null)
            {
                return listResources(new JarInputStream(jarUrl.openStream()), path);
            }
            InputStream is = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> resources = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null)
            {
                //如果是类的话
                if (line.indexOf(".class") > 0)
                {
                    resources.add(path + "/" + line);
                    // 是目录的话
                }
                else
                {
                    List<String> children = listResource(path + "/" + line);
                    if (children != null && children.size() > 0)
                    {
                        resources.addAll(children);
                    }
                }
            }
            reader.close();
            return resources;
        }
        return null;
    }

    private URL findJarForResource(URL url) throws IOException
    {
        String jarUrl = url.getPath();
        int index = jarUrl.indexOf(".jar");
        if (index < 0)
        {
            return null;
        }

        jarUrl = jarUrl.substring(0, index + 4);
        URL testUrl = new URL(jarUrl);
        if (isJar(testUrl))
        {
            return testUrl;
        }
        return null;
    }

    // 通过文件的魔数来判断是否是jar 文件
    private boolean isJar(URL testUrl) throws IOException
    {
        InputStream inputStream = testUrl.openStream();
        byte[] buffer = new byte[JAR_MAGIC.length];
        inputStream.read(buffer, 0, JAR_MAGIC.length);
        if (Arrays.equals(JAR_MAGIC, buffer))
        {
            return true;
        }
        inputStream.close();
        return false;
    }

    // 通过jar 来获取resourced
    private List<String> listResources(JarInputStream jar, String path) throws IOException
    {
        //为path 在开头和末尾添加 /
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        if (!path.endsWith("/"))
        {
            path = path + "/";
        }

        List<String> resources = new ArrayList<>();
        for (JarEntry entry; (entry = jar.getNextJarEntry()) != null; )
        {
            if (!entry.isDirectory())
            {
                StringBuilder name = new StringBuilder(entry.getName());
                // 进行文件名的统一
                if (name.charAt(0) != '/')
                {
                    name.insert(0, '/');
                }

                if (name.indexOf(path) == 0)
                {
                    // 去掉开头的斜杠
                    resources.add(name.substring(1));
                }
            }
        }
        return resources;
    }
}
