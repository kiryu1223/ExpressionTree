# ExpressionTree
支持jdk8至21

jdk15后的版本(**不包括jdk15**)因为封装规则的修改，需要往项目根目录下的.mvn/jvm.config
文件里写入以下指令,可能会提示报错，这是正常的
> --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
>
> --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
>
> --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
> 
> --add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED

## maven
```xml
<dependency>
  <groupId>io.github.kiryu1223</groupId>
  <artifactId>ExpressionTree</artifactId>
  <version>1.1.0</version>
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
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 表达式对象
+ #### ExprTree
  核心类，该类的构造函数为一个lambda表达式，表达式插件会在代码编译时读取lambda的内容并生成表达式对象，
  以构造函数的形式记录在第二个参数里，运行时可以获取代码的表达式对象
+ #### Expression
  表达式对象抽象基类，内置生成所有表达式对象的工厂方法以及访问者模式，
  每个实现类都实现了获取自身携带参数和自己的类型的方法

