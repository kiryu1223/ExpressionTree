# ExpressionTree

qq交流群：497125166

支持jdk8至21

jdk15后的版本(**不包括jdk15**)因为封装规则的修改，需要往项目根目录下的.mvn/jvm.config
文件里写入以下指令,可能会提示报错，这是正常的


**注意了注意了‼️‼️‼️**
<br>因为升级实现的缘故，暂时把主动创建ExprTree对象的形式给不支持了，等哪天有空了再加回来


```text
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED
```

## maven
```xml
<dependency>
  <groupId>io.github.kiryu1223</groupId>
  <artifactId>ExpressionTree</artifactId>
  <version>1.3.2</version>
</dependency>
```

### 如何添加到项目
1. 引入依赖
2. 在build-plugins-plugin-configuration-compilerArgs添加插件开启指令-Xplugin:ExpressionTree，具体如下
```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <compilerArgs>
                    <arg>-Xplugin:ExpressionTree</arg>
                </compilerArgs>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.easy-query</groupId>
                        <artifactId>sql-api-lambda</artifactId>
                        <version>2.0.31</version>
                    </path>
                    <!-- 你的其他注解处理器，比如说lombok -->
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```
### 注解
+ @Expr
  为你需要替换lambda参数为表达式树的方法形参上加入此注解，该方法的其余参数必须一致

### 表达式对象
+ #### Expression
  表达式对象抽象基类，内置生成所有表达式对象的工厂方法以及访问者模式，
  每个实现类都实现了获取自身携带参数和自己的类型的方法

### 简单的使用案例
```java
package org.example;

import io.github.kiryu1223.expressionTree.delegate.Action1;
import io.github.kiryu1223.expressionTree.expressions.ExprTree;

import static io.github.kiryu1223.expressionTree.expressions.ExprTree.Expr;

public class Main
{
    public static void main(String[] args)
    {
        test(s -> System.out.println(s));
    }

    static void test(@Expr Action1<String> action)
    {
        throw new RuntimeException();
    }

    static void test(ExprTree<Action1<String>> action)
    {
        System.out.println("lambda代码体为:" + action.getTree());
    }
}
```
执行结果
```text
lambda代码体为:(s) -> {
    System.out.println(s);
}
```

