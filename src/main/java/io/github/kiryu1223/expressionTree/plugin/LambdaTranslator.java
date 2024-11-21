package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.delegate.Delegate;
import io.github.kiryu1223.expressionTree.expressions.*;
import io.github.kiryu1223.expressionTree.expressions.annos.Expr;
import io.github.kiryu1223.expressionTree.util.JDK;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import javax.lang.model.element.ElementKind;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.kiryu1223.expressionTree.expressions.Kind.*;

public class LambdaTranslator extends TreeTranslator
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Symtab symtab;
    private final ClassReader classReader;
    private final Object moduleSymbol;
    private final ArrayDeque<Symbol> thizDeque;
    private final ArrayDeque<Symbol> ownerDeque;
    private final ArrayDeque<Symbol.VarSymbol> varSymbolDeque;
    private final ArrayDeque<ListBuffer<JCTree.JCStatement>> statementsDeque;
    private final AtomicInteger argIndex;
    private final Map<Name, JCTree.JCVariableDecl> lambdaVarMap;
    private final Map<JCTree.JCLambda, JCTree.JCVariableDecl> lambdaCache;
    private final ArrayDeque<ListBuffer<Name>> argNameDeque;

    public LambdaTranslator(TreeMaker treeMaker, Types types, Names names, Symtab symtab, ClassReader classReader, Object moduleSymbol, ArrayDeque<Symbol> thizDeque, ArrayDeque<Symbol> ownerDeque, ArrayDeque<Symbol.VarSymbol> varSymbolDeque, ArrayDeque<ListBuffer<JCTree.JCStatement>> statementsDeque, AtomicInteger argIndex, Map<Name, JCTree.JCVariableDecl> lambdaVarMap, Map<JCTree.JCLambda, JCTree.JCVariableDecl> lambdaCache, ArrayDeque<ListBuffer<Name>> argNameDeque)
    {
        this.treeMaker = treeMaker;
        this.types = types;
        this.names = names;
        this.symtab = symtab;
        this.classReader = classReader;
        this.moduleSymbol = moduleSymbol;
        this.thizDeque = thizDeque;
        this.ownerDeque = ownerDeque;
        this.varSymbolDeque = varSymbolDeque;
        this.statementsDeque = statementsDeque;
        this.argIndex = argIndex;
        this.lambdaVarMap = lambdaVarMap;
        this.lambdaCache = lambdaCache;
        this.argNameDeque = argNameDeque;
    }

    public JCTree.JCExpression translateV(JCTree tree)
    {
        if (tree == null)
        {
            return null;
        }
        else
        {
            tree.accept(this);
            JCTree tmpResult = this.result;
            this.result = null;
            return (JCTree.JCExpression) tmpResult;
        }
    }

    // lambda
    @Override
    public void visitLambda(JCTree.JCLambda tree)
    {
        Symbol.VarSymbol varSymbol = varSymbolDeque.peek();
        // 检查是否需要进行表达式树展开
        if (varSymbol == null || varSymbol.getAnnotation(Expr.class) == null)
        {
            // 表达式lambda必须包装成大括号lambda才能正常工作
            tryOpenLambda(tree);
            tree.body = translate(tree.body);
            result = tree;
        }
        else
        {
            Expr expr = varSymbol.getAnnotation(Expr.class);
            // 检测lambda类型是否符合约束
            checkBody(expr.value(), tree.getBodyKind(), tree);
            // 收集lambda参数
            ListBuffer<Name> nameList = new ListBuffer<>();
            for (JCTree.JCVariableDecl param : tree.params)
            {
                // 为每个lambda入参创建一个局部变量
                JCTree.JCVariableDecl localVar = getLocalVar(param.type, param.getName().toString());
                // 以入参名作为key
                lambdaVarMap.put(param.name, localVar);
                // 记录一下入参名
                nameList.add(param.name);
            }
            JCTree.JCExpression translate = (JCTree.JCExpression) translate(tree.body);

            Symbol.MethodSymbol exprSymbol = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
            JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), exprSymbol);
            result = treeMaker.App(fa, List.of(tree, translate));

            // 归还lambda参数
            for (Name name : nameList)
            {
                lambdaVarMap.remove(name);
            }
        }
    }

    // 代码块
    @Override
    public void visitBlock(JCTree.JCBlock tree)
    {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        ListBuffer<Name> locals = new ListBuffer<>();
        argNameDeque.push(locals);
        for (JCTree statement : tree.getStatements())
        {
            args.append(translateV(statement));
        }
        for (Name local : locals)
        {
            lambdaVarMap.remove(local);
        }
        argNameDeque.pop();
        result = treeMaker.App(
                getFactoryMethod(Block, Arrays.asList(Expression[].class, boolean.class)),
                List.of(
                        makeArray(Expression.class, args.toList()),
                        treeMaker.Literal(tree.isStatic())
                )
        );
    }

    // 局部变量
    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree)
    {
        // 记录局部变量
        ListBuffer<Name> peek = argNameDeque.peek();
        peek.add(tree.getName());
        lambdaVarMap.put(tree.getName(), tree);

        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        args.append(treeMaker.Ident(lambdaVarMap.get(tree.getName())));
        JCTree.JCExpression initializer = tree.getInitializer();
        if (initializer != null)
        {
            args.append(translateV(initializer));
        }
        else
        {
            args.append(getNull());
        }
        result = treeMaker.App(
                getFactoryMethod(Variable, Arrays.asList(ParameterExpression.class, Expression.class)),
                args.toList()
        );
    }

    // 基本类型
    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree tree)
    {
        result = treeMaker.App(
                getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                List.of(treeMaker.ClassLiteral(tree.type))
        );
    }

    // 标识符
    @Override
    public void visitIdent(JCTree.JCIdent tree)
    {
        if (tree.sym.getKind().isClass() || tree.sym.getKind().isInterface())
        {
            result = treeMaker.App(
                    getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                    List.of(treeMaker.ClassLiteral(tree.type))
            );
        }
        else if (lambdaVarMap.containsKey(tree.getName()))
        {
            JCTree.JCVariableDecl jcVariableDecl = lambdaVarMap.get(tree.getName());
            result = treeMaker.Ident(jcVariableDecl);
        }
        else
        {
            result = treeMaker.App(
                    getFactoryMethod(Reference, Arrays.asList(Object.class, String.class)),
                    List.of(tree, treeMaker.Literal(tree.getName().toString()))
            );
        }
    }

    // 常量
    @Override
    public void visitLiteral(JCTree.JCLiteral tree)
    {
        result = treeMaker.App(getFactoryMethod(Constant, Collections.singletonList(Object.class)), List.of(tree));
    }

    // 访问选择
    @Override
    public void visitSelect(JCTree.JCFieldAccess tree)
    {
        JCTree.JCExpression left = tree.getExpression();
        Name right = tree.getIdentifier();
        if (tree.sym.getKind() == ElementKind.FIELD
                // class是关键字不能作为字段和函数名，可以直接判断
                && tree.getIdentifier().toString().equals("class"))
        {
            result = treeMaker.App(
                    getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                    List.of(treeMaker.ClassLiteral(left.type))
            );
        }
        else
        {
            result = treeMaker.App(
                    getFactoryMethod(FieldSelect, Arrays.asList(Expression.class, Field.class)),
                    List.of(
                            translateV(left),
                            reflectField(left.type, right.toString())
                    )
            );
        }
    }

    // 括号
    @Override
    public void visitParens(JCTree.JCParens tree)
    {
        JCTree.JCExpression expr = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(Parens, Collections.singletonList(Expression.class)),
                List.of(expr)
        );
    }

    // region [Util]
    private void tryOpenLambda(JCTree.JCLambda tree)
    {
        if (tree.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION)
        {
            JCTree.JCExpression body = (JCTree.JCExpression) tree.getBody();
            Type lambdaReturnType = getLambdaReturnType(tree);
            // (...) void
            if (lambdaReturnType == symtab.voidType)
            {
                JCTree.JCExpressionStatement exec = treeMaker.Exec(body);
                tree.body = treeMaker.Block(0, List.of(exec));
            }
            // (...) not void
            else
            {
                JCTree.JCReturn aReturn = treeMaker.Return(body);
                tree.body = treeMaker.Block(0, List.of(aReturn));
            }
        }
    }

    private Type getLambdaReturnType(JCTree.JCLambda lambda)
    {
        Type descriptorType = types.findDescriptorType(lambda.type);
        Type.MethodType methodType = descriptorType.asMethodType();
        return methodType.getReturnType();
    }

    private JCTree.JCVariableDecl getLocalLambdaExpr(JCTree.JCExpression body, ListBuffer<JCTree.JCExpression> args, Type returnType, Type gt)
    {
        Type.ClassType classType = new Type.ClassType(
                Type.noType,
                List.of(gt),
                getClassSymbol(LambdaExpression.class)
        );
        return treeMaker.VarDef(
                new Symbol.VarSymbol(
                        Flags.HASINIT + Flags.EFFECTIVELY_FINAL,
                        names.fromString(getNextLambdaParameter()),
                        classType,
                        ownerDeque.peek()
                ),
                treeMaker.App(
                        getFactoryMethod(Lambda, Arrays.asList(Expression.class, ParameterExpression[].class, Class.class)),
                        List.of(
                                body,
                                makeArray(ParameterExpression.class, args.toList()),
                                treeMaker.ClassLiteral(returnType)
                        )
                ));
    }

    private Symbol.MethodSymbol methodInvocationGetMethodSymbol(JCTree.JCMethodInvocation tree)
    {
        JCTree.JCExpression methodSelect = tree.getMethodSelect();
        Symbol.MethodSymbol methodSymbol;
        if (methodSelect instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
            methodSymbol = (Symbol.MethodSymbol) select.sym;
        }
        else
        {
            JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
            methodSymbol = (Symbol.MethodSymbol) select.sym;
        }
        return methodSymbol;
    }

    private void checkBody(Expr.BodyType value, LambdaExpressionTree.BodyKind bodyKind, JCTree.JCLambda lambda)
    {
        if ((value == Expr.BodyType.Expr && bodyKind == LambdaExpressionTree.BodyKind.STATEMENT)
                || (value == Expr.BodyType.Block && bodyKind == LambdaExpressionTree.BodyKind.EXPRESSION))
        {
            throw new RuntimeException(String.format("期望的lambda类型为: %s,实际为: %s\n%s", value == Expr.BodyType.Expr ? "表达式" : "代码块", value == Expr.BodyType.Expr ? "代码块" : "表达式", lambda));
        }
    }

    private Type getType(Class<?> clazz)
    {
        if (clazz.isPrimitive())
        {
            if (clazz == int.class) return symtab.intType;
            if (clazz == byte.class) return symtab.byteType;
            if (clazz == short.class) return symtab.shortType;
            if (clazz == long.class) return symtab.longType;
            if (clazz == boolean.class) return symtab.booleanType;
            if (clazz == char.class) return symtab.charType;
            if (clazz == float.class) return symtab.floatType;
            if (clazz == double.class) return symtab.doubleType;
            if (clazz == void.class) return symtab.voidType;
        }
        return getClassSymbol(clazz).asType();
    }

    private JCTree.JCFieldAccess getFactoryMethod(Kind methodType, java.util.List<Class<?>> argTypes)
    {
        ListBuffer<Type> typeListBuffer = new ListBuffer<>();
        for (Class<?> as : argTypes)
        {
            if (as.isArray())
            {
                Class<?> componentType = as.getComponentType();
                Type.ArrayType arrayType = types.makeArrayType(getType(componentType));
                typeListBuffer.append(arrayType);
            }
            else
            {
                typeListBuffer.append(getType(as));
            }
        }
        return getFactoryMethod(methodType, typeListBuffer.toList());
    }

    private JCTree.JCFieldAccess getFactoryMethod(Kind methodType, List<Type> argTypes)
    {
        Name name = names.fromString(methodType.name());
//        ListBuffer<Type> argTypes = new ListBuffer<>();
//        for (Class<?> as : args)
//        {
//            if (as.isArray())
//            {
//                Class<?> componentType = as.getComponentType();
//                Type.ArrayType arrayType = types.makeArrayType(getType(componentType));
//                argTypes.append(arrayType);
//            }
//            else
//            {
//                argTypes.append(getType(as));
//            }
//        }
        Symbol.ClassSymbol classSymbol = getClassSymbol(Expression.class);
        Symbol.MethodSymbol methodSymbol = null;
        for (Symbol enclosedElement : classSymbol.getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol element = (Symbol.MethodSymbol) enclosedElement;
            if (!element.getSimpleName().equals(name)) continue;
            List<Symbol.VarSymbol> parameters = element.getParameters();
            if (parameters.isEmpty() && argTypes.isEmpty())
            {
                methodSymbol = element;
                break;
            }
            if (parameters.size() != argTypes.size()) continue;
            ListBuffer<Type> vars = new ListBuffer<>();
            for (Symbol.VarSymbol parameter : parameters)
            {
                vars.append(parameter.asType());
            }
            if (!types.isSubtypes(argTypes, vars.toList())) continue;
            methodSymbol = element;
        }
        if (methodSymbol != null)
        {
            return refMakeSelector(treeMaker.Ident(classSymbol), methodSymbol);
        }
        throw new RuntimeException(String.format("getFactoryMethod方法无法获取到函数\n 函数名为:%s\n 参数类型为:%s\n", methodType, argTypes));
    }

    private JCTree.JCFieldAccess refMakeSelector(JCTree.JCExpression base, Symbol sym)
    {
        return ReflectUtil.invokeMethod(treeMaker, "Select", Arrays.asList(base, sym));
    }

    private JCTree.JCFieldAccess getOperator(JCTree.Tag tag)
    {
        return getOperator(tag.name());
    }

    private JCTree.JCFieldAccess getOperator(OperatorType operatorType)
    {
        return getOperator(operatorType.name());
    }

    private JCTree.JCFieldAccess getOperator(String op)
    {
        Symbol.ClassSymbol classSymbol = getClassSymbol(OperatorType.class);
        for (Symbol enclosedElement : classSymbol.getEnclosedElements())
        {
            if (enclosedElement.getKind() != ElementKind.ENUM_CONSTANT) continue;
            if (!op.equals(enclosedElement.getSimpleName().toString())) continue;
            return refMakeSelector(treeMaker.Ident(classSymbol), enclosedElement);
        }
        throw new RuntimeException("getOperator " + classSymbol);
    }

    private Symbol.ClassSymbol getClassSymbol(Class<?> clazz)
    {
        Name name = names.fromString(clazz.getName());
        Symbol.ClassSymbol classSymbol;
        if (JDK.is9orLater())
        {
            classSymbol = ReflectUtil.invokeMethod(symtab, "enterClass", Arrays.asList(moduleSymbol, name));
        }
        else
        {
            classSymbol = classReader.enterClass(name);
        }
        return classSymbol;
    }

    private Symbol.MethodSymbol getMethodSymbol(Class<?> clazz, String methodName, java.util.List<Class<?>> args)
    {
        Name name = names.fromString(methodName);
        ListBuffer<Type> argTypes = new ListBuffer<>();
        for (Class<?> as : args)
        {
            if (as.isArray())
            {
                Class<?> componentType = as.getComponentType();
                Type.ArrayType arrayType = types.makeArrayType(getType(componentType));
                argTypes.append(arrayType);
            }
            else
            {
                argTypes.append(getType(as));
            }
        }
        for (Symbol enclosedElement : getClassSymbol(clazz).getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) enclosedElement;
            if (!methodSymbol.getSimpleName().equals(name)) continue;
            if (methodSymbol.getParameters().size() != argTypes.size()) continue;
            List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();
            ListBuffer<Type> vars = new ListBuffer<>();
            for (Symbol.VarSymbol parameter : parameters)
            {
                Type type = parameter.asType();
                vars.append(types.erasure(type));
            }
            if (!types.isSubtypes(argTypes.toList(), vars.toList())) continue;
            return methodSymbol;
        }
        throw new RuntimeException(String.format("getMethodSymbol方法无法获取到函数\n 目标类为:%s\n 函数名为:%s\n 参数类型为:%s\n", clazz, methodName, args));
    }

    private JCTree.JCMethodInvocation reflectMethod(Type type, String name, ListBuffer<JCTree.JCExpression> args)
    {
        return reflectMethod(type, name, args.toList());
    }

    private JCTree.JCMethodInvocation reflectMethod(Type type, String name, List<JCTree.JCExpression> args)
    {
        return treeMaker.App(
                refMakeSelector(
                        treeMaker.Ident(getClassSymbol(ReflectUtil.class)),
                        getMethodSymbol(
                                ReflectUtil.class,
                                "getMethod",
                                Arrays.asList(Class.class, String.class, Class[].class)
                        )
                ),
                List.of(
                        treeMaker.ClassLiteral(type),
                        treeMaker.Literal(name),
                        makeArray(Class.class, args)
                )
        );
    }

    private JCTree.JCMethodInvocation reflectField(Type type, String name)
    {
        return treeMaker.App(
                refMakeSelector(
                        treeMaker.Ident(getClassSymbol(ReflectUtil.class)),
                        getMethodSymbol(ReflectUtil.class, "getField", Arrays.asList(Class.class, String.class))
                ),
                List.of(
                        treeMaker.ClassLiteral(type),
                        treeMaker.Literal(name)
                )
        );
    }

    private JCTree.JCNewArray makeArray(Class<?> clazz, List<JCTree.JCExpression> args)
    {
        return (JCTree.JCNewArray) treeMaker.NewArray(treeMaker.Ident(getClassSymbol(clazz)), List.nil(), args)
                .setType(types.makeArrayType(getType(clazz)));
    }

    private JCTree.JCVariableDecl getLocalVar(Type type, String name)
    {
        return treeMaker.VarDef(
                new Symbol.VarSymbol(
                        Flags.HASINIT + Flags.EFFECTIVELY_FINAL,
                        names.fromString(getNextLambdaParameter()),
                        getType(ParameterExpression.class),
                        ownerDeque.peek()
                ),
                treeMaker.App(
                        getFactoryMethod(Parameter, Arrays.asList(Class.class, String.class)),
                        List.of(
                                treeMaker.ClassLiteral(type),
                                treeMaker.Literal(name)
                        )
                ));
    }

    private String getNextLambdaParameter()
    {
        return "lambdaParameter_" + argIndex.getAndIncrement();
    }

    private JCTree.JCLiteral getNull()
    {
        return treeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
    }

    private JCTree.JCMethodInvocation reflectConstructor(Type type, ListBuffer<JCTree.JCExpression> args)
    {
        return treeMaker.App(
                refMakeSelector(
                        treeMaker.Ident(getClassSymbol(ReflectUtil.class)),
                        getMethodSymbol(
                                ReflectUtil.class,
                                "getConstructor",
                                Arrays.asList(Class.class, Class[].class)
                        )
                ),
                List.of(
                        treeMaker.ClassLiteral(type),
                        makeArray(Class.class, args.toList())
                )
        );
    }

    private Symbol.MethodSymbol getMethodSymbol(Symbol classSymbol, Name methodName, Type.MethodType methodType)
    {
        for (Symbol enclosedElement : classSymbol.getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) enclosedElement;
            if (!methodSymbol.getSimpleName().equals(methodName)) continue;
            Type methodSymbolType = methodSymbol.asType();
            List<Type> parameterTypes1 = methodSymbolType.getParameterTypes();
            List<Type> parameterTypes2 = methodType.getParameterTypes();
            if (types.isSubtypes(parameterTypes2, types.erasure(parameterTypes1)))
            {
                return methodSymbol;
            }
        }

        throw new RuntimeException(String.format("getMethodSymbol方法无法获取到函数\n 目标类为:%s\n 函数名为:%s\n 函数类型:%s\n", classSymbol, methodName, methodType));
    }

    // endregion
}
