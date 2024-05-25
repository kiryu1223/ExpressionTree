package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import io.github.kiryu1223.expressionTree.delegate.Delegate;
import io.github.kiryu1223.expressionTree.expressions.*;
import io.github.kiryu1223.expressionTree.util.JDK;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static io.github.kiryu1223.expressionTree.expressions.Kind.*;

public class SugarScanner extends TreeScanner
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Symtab symtab;
    private final JavaCompiler javaCompiler;
    private final Object moduleSymbol;
    private final Type thiz;
    private int index = 0;

    public SugarScanner(Type thiz, Context context, Object moduleSymbol)
    {
        this.thiz = thiz;
        treeMaker = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
        javaCompiler = JavaCompiler.instance(context);
        this.moduleSymbol = moduleSymbol;
    }

    public SugarScanner(Type thiz, Context context)
    {
        this(thiz, context, null);
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl methodDecl)
    {
        JCTree.JCBlock body = methodDecl.getBody();
        if (body == null) return;
        treeMaker.at(methodDecl.pos);
        index = 0;
        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        for (JCTree.JCStatement statement : body.getStatements())
        {
            statement.accept(new SugarTranslator(jcStatements, methodDecl.sym));
            jcStatements.append(statement);
        }
        methodDecl.body.stats = jcStatements.toList();
    }

    @Override
    public void visitBlock(JCTree.JCBlock block)
    {
        if (block.isStatic() && hasTaskMake(block))
        {
            treeMaker.at(block.pos);
            index = 0;
            List<JCTree.JCStatement> statements = block.getStatements();
            JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) statements.get(0);
            Symbol owner = variableDecl.sym.location();
            ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
            for (JCTree.JCStatement statement : statements)
            {
                statement.accept(new SugarTranslator(jcStatements, owner));
                jcStatements.append(statement);
            }
            block.stats = jcStatements.toList();
        }
    }

    private boolean hasTaskMake(JCTree.JCBlock block)
    {
        List<JCTree.JCStatement> statements = block.getStatements();
        if (statements.isEmpty()) return false;
        JCTree.JCStatement jcStatement = statements.get(0);
        if (!(jcStatement instanceof JCTree.JCVariableDecl)) return false;
        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) jcStatement;
        return variableDecl.getName().toString().equals("taskMake")
                || (variableDecl.getType() instanceof JCTree.JCPrimitiveTypeTree
                && ((JCTree.JCPrimitiveTypeTree) variableDecl.getType()).getPrimitiveTypeKind() == TypeKind.INT);
    }

    // todo:将来加入严格检查，现在开摆
    private boolean checkExprAnno(Symbol.MethodSymbol symbol, int index)
    {
        Symbol.VarSymbol varSymbol = symbol.getParameters().get(index);
        return varSymbol.getAnnotation(Expr.class) != null;
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

        throw new RuntimeException(String.format("getMethodSymbol方法无法获取到函数\n 目标类为:%s\n 函数名为:%s\n 方法类型:%s\n", classSymbol, methodName, methodType));
    }

    private Symbol.MethodSymbol getMethodSymbol(Symbol classSymbol, Name methodName, java.util.List<JCTree.JCExpression> args)
    {
        ListBuffer<Type> argTypes = new ListBuffer<>();
        for (JCTree.JCExpression expression : args)
        {
            argTypes.append(expression.type);
        }
        List<Type> typeList = argTypes.toList();
        for (Symbol enclosedElement : classSymbol.getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) enclosedElement;
            if (!methodSymbol.getSimpleName().equals(methodName)) continue;
            Type methodSymbolType = methodSymbol.asType();
            List<Type> parameterTypes = methodSymbolType.getParameterTypes();
            if (types.isSubtypes(typeList, types.erasure(parameterTypes)))
            {
                return methodSymbol;
            }
        }

        throw new RuntimeException(String.format("getMethodSymbol方法无法获取到函数\n 目标类为:%s\n 函数名为:%s\n 方法类型:%s\n", classSymbol, methodName, args));
    }

    private Symbol.ClassSymbol getClassSymbol(Class<?> clazz)
    {
        Name name = names.fromString(clazz.getName());
        Symbol.ClassSymbol classSymbol;
        if (JDK.is9orLater())
        {
            classSymbol = ReflectUtil.invokeMethod(symtab, "getClass", Arrays.asList(moduleSymbol, name));
            if (classSymbol == null)
            {
                classSymbol = ReflectUtil.invokeMethod(javaCompiler, "resolveIdent", Arrays.asList(moduleSymbol, clazz.getName()));
            }
        }
        else
        {
            classSymbol = ReflectUtil.<Map<Name, Symbol.ClassSymbol>>getFieldValue(symtab, "classes").get(name);
            if (classSymbol == null)
            {
                classSymbol = ReflectUtil.invokeMethod(javaCompiler, "resolveIdent", Collections.singletonList(clazz.getName()));
            }
        }
        return classSymbol;
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

    private JCTree.JCFieldAccess getFactoryMethod(Kind methodType, java.util.List<Class<?>> args)
    {
        Name name = names.fromString(methodType.name());
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
        Symbol.ClassSymbol classSymbol = getClassSymbol(Expression.class);
        Symbol.MethodSymbol methodSymbol = null;
        for (Symbol enclosedElement : classSymbol.getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol element = (Symbol.MethodSymbol) enclosedElement;
            if (!element.getSimpleName().equals(name)) continue;
            List<Symbol.VarSymbol> parameters = element.getParameters();
            if (parameters.isEmpty() && args.isEmpty())
            {
                methodSymbol = element;
                break;
            }
            if (parameters.size() != args.size()) continue;
            ListBuffer<Type> vars = new ListBuffer<>();
            for (Symbol.VarSymbol parameter : parameters)
            {
                vars.append(parameter.asType());
            }
            if (!types.isSubtypes(argTypes.toList(), vars.toList())) continue;
            methodSymbol = element;
        }
        if (methodSymbol != null)
        {
            return refMakeSelector(treeMaker.Ident(classSymbol), methodSymbol);
        }
        throw new RuntimeException(String.format("getFactoryMethod方法无法获取到函数\n 函数名为:%s\n 参数类型为:%s\n", methodType, args));
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

//    private JCTree.JCFieldAccess to_class(Type type)
//    {
//        Symbol.ClassSymbol classSymbol;
//        if (type.isPrimitiveOrVoid())
//        {
//            classSymbol = types.boxedClass(type);
//        }
//        else
//        {
//            classSymbol = getClassSymbol(type);
//        }
//        Type.ClassType classType = new Type.ClassType(Type.noType, List.of(type), getClassSymbol(Class.class));
//        Symbol.VarSymbol _class = new Symbol.VarSymbol(Flags.InterfaceVarFlags, type.isPrimitiveOrVoid() ? names.TYPE : names._class, classType, classSymbol);
//        return (JCTree.JCFieldAccess) treeMaker.Select(treeMaker.Ident(classSymbol), _class);
//    }

    private JCTree.JCNewArray makeArray(Class<?> clazz, List<JCTree.JCExpression> args)
    {
        return (JCTree.JCNewArray) treeMaker.NewArray(treeMaker.Ident(getClassSymbol(clazz)), List.nil(), args)
                .setType(types.makeArrayType(getType(clazz)));
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

    private JCTree.JCMethodInvocation reflectMethod(Type type, String name, ListBuffer<JCTree.JCExpression> args)
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
                        makeArray(Class.class, args.toList())
                )
        );
    }

    private JCTree.JCLiteral getNull()
    {
        return treeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
    }

    private class SugarTranslator extends TreeTranslator
    {
        private final ListBuffer<JCTree.JCStatement> jcStatements;
        private final Map<Name, JCTree.JCVariableDecl> variableDeclMap = new HashMap<>();
        private final Map<Name, JCTree.JCVariableDecl> tempVariableDeclMap = new HashMap<>();
        private final Map<JCTree.JCLambda, JCTree.JCVariableDecl> tempLambdaMap = new HashMap<>();
        private final Symbol owner;

        public SugarTranslator(ListBuffer<JCTree.JCStatement> jcStatements, Symbol owner)
        {
            this.jcStatements = jcStatements;
            this.owner = owner;
        }

        @Override
        public void visitApply(JCTree.JCMethodInvocation invocation)
        {
            super.visitApply(invocation);
            JCTree.JCExpression methodSelect = invocation.getMethodSelect();
            List<JCTree.JCExpression> arguments = invocation.getArguments();
            if (!arguments.isEmpty())
            {
                boolean flag = false;
                Symbol symbol;
                if (methodSelect instanceof JCTree.JCFieldAccess)
                {
                    JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
                    symbol = select.sym;
                }
                else
                {
                    JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
                    symbol = select.sym;
                }
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (int i = 0; i < arguments.size(); i++)
                {
                    JCTree.JCExpression jcExpression = arguments.get(i);
                    if (jcExpression.getKind() != Tree.Kind.LAMBDA_EXPRESSION
                            || !checkExprAnno((Symbol.MethodSymbol) symbol, i))
                    {
                        args.append(jcExpression);
                        continue;
                    }
                    JCTree.JCLambda lambda = (JCTree.JCLambda) jcExpression;
                    flag = true;
                    JCTree.JCExpression built = buildExpr(lambda);
                    Symbol.MethodSymbol expr = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
                    JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), expr);
                    JCTree.JCMethodInvocation apply = treeMaker.App(fa, List.of(jcExpression, built));
                    args.append(apply);
                }
                if (flag)
                {
                    Symbol.MethodSymbol methodSymbol = getMethodSymbol(
                            (Symbol.ClassSymbol) symbol.location(),
                            methodSelect instanceof JCTree.JCFieldAccess
                                    ? ((JCTree.JCFieldAccess) methodSelect).getIdentifier()
                                    : ((JCTree.JCIdent) methodSelect).getName(),
                            args.toList()
                    );
                    result = treeMaker.App(
                            methodSelect instanceof JCTree.JCFieldAccess
                                    ? refMakeSelector(((JCTree.JCFieldAccess) methodSelect).getExpression(), methodSymbol)
                                    : treeMaker.Ident(methodSymbol),
                            args.toList());
                }
            }
        }

        private String getNextLambdaParameter()
        {
            return "lambdaParameter_" + index++;
        }

        private JCTree.JCVariableDecl getLocalVar(Type type, String name)
        {
            return treeMaker.VarDef(
                    new Symbol.VarSymbol(
                            Flags.HASINIT + Flags.EFFECTIVELY_FINAL,
                            names.fromString(getNextLambdaParameter()),
                            getType(ParameterExpression.class),
                            owner
                    ),
                    treeMaker.App(
                            getFactoryMethod(Parameter, Arrays.asList(Class.class, String.class)),
                            List.of(
                                    treeMaker.ClassLiteral(type),
                                    treeMaker.Literal(name)
                            )
                    ));
        }

        private JCTree.JCVariableDecl getLocalLambdaExpr(JCTree.JCExpression body, ListBuffer<JCTree.JCExpression> args, Type returnType, Type gt)
        {
            Type type = returnType;

//            if (returnType instanceof Type.ClassType)
//            {
//                Type.ClassType classType = (Type.ClassType) returnType;
//                type = classType.asElement().isAnonymous() ?
//                        classType.supertype_field :
//                        classType;
//            }
//            else
//            {
//                type = returnType;
//            }

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
                            owner
                    ),
                    treeMaker.App(
                            getFactoryMethod(Lambda, Arrays.asList(Expression.class, ParameterExpression[].class, Class.class)),
                            List.of(
                                    body,
                                    makeArray(ParameterExpression.class, args.toList()),
                                    treeMaker.ClassLiteral(type)
                            )
                    ));
        }

        private JCTree.JCExpression buildExpr(JCTree.JCLambda lambda)
        {
            return deepMake(lambda);
        }

        private JCTree.JCExpression deepMake(JCTree tree)
        {
            if (tree instanceof JCTree.JCPrimitiveTypeTree)
            {
                JCTree.JCPrimitiveTypeTree jcPrimitiveTypeTree = (JCTree.JCPrimitiveTypeTree) tree;
                return treeMaker.App(
                        getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                        List.of(treeMaker.ClassLiteral(jcPrimitiveTypeTree.type))
                );
            }
            else if (tree instanceof JCTree.JCLiteral)
            {
                JCTree.JCLiteral jcLiteral = (JCTree.JCLiteral) tree;
                return treeMaker.App(getFactoryMethod(Constant, Collections.singletonList(Object.class)), List.of(jcLiteral));
            }
            else if (tree instanceof JCTree.JCIdent)
            {
                JCTree.JCIdent jcIdent = (JCTree.JCIdent) tree;
                if (jcIdent.sym.getKind() == ElementKind.CLASS)
                {
                    return treeMaker.App(
                            getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                            List.of(treeMaker.ClassLiteral(jcIdent.type))
                    );
                }
                else if (variableDeclMap.containsKey(jcIdent.getName()))
                {
                    return treeMaker.Ident(variableDeclMap.get(jcIdent.getName()));
                }
                else if (tempVariableDeclMap.containsKey(jcIdent.getName()))
                {
                    return treeMaker.Ident(tempVariableDeclMap.get(jcIdent.getName()));
                }
                else
                {
                    return treeMaker.App(
                            getFactoryMethod(Reference, Arrays.asList(Object.class, String.class)),
                            List.of(jcIdent, treeMaker.Literal(jcIdent.getName().toString()))
                    );
                }
            }
            else if (tree instanceof JCTree.JCBinary)
            {
                JCTree.JCBinary jcBinary = (JCTree.JCBinary) tree;
                JCTree.JCExpression left = deepMake(jcBinary.getLeftOperand());
                JCTree.JCExpression right = deepMake(jcBinary.getRightOperand());
                return treeMaker.App(
                        getFactoryMethod(Binary, Arrays.asList(Expression.class, Expression.class, OperatorType.class)),
                        List.of(left, right, getOperator(jcBinary.getTag()))
                );
            }
            else if (tree instanceof JCTree.JCMethodInvocation)
            {
                JCTree.JCMethodInvocation jcMethodInvocation = (JCTree.JCMethodInvocation) tree;
                JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
                ListBuffer<JCTree.JCExpression> ts = new ListBuffer<>();
                for (Type parameterType : methodSelect.type.getParameterTypes())
                {
                    ts.append(treeMaker.ClassLiteral(parameterType));
                }
                List<JCTree.JCExpression> arguments = jcMethodInvocation.getArguments();
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression argument : arguments)
                {
                    if (argument instanceof JCTree.JCMethodInvocation &&
                            ((JCTree.JCMethodInvocation) argument).getArguments().size() == 2 &&
                            ((JCTree.JCMethodInvocation) argument).getArguments().get(0).getKind() == Tree.Kind.LAMBDA_EXPRESSION &&
                            argument.type.tsym.equals(getClassSymbol(ExprTree.class)))
                    {
                        JCTree.JCMethodInvocation invoke = (JCTree.JCMethodInvocation) argument;
                        JCTree.JCLambda lambda = (JCTree.JCLambda) invoke.getArguments().get(0);
                        JCTree.JCVariableDecl variableDecl = tempLambdaMap.get(lambda);
                        JCTree.JCExpression ag = treeMaker.Ident(variableDecl);
                        args.append(ag);
                    }
                    else
                    {
                        args.append(deepMake(argument));
                    }
                }

                java.util.List<Class<?>> argTypes = new ArrayList<>(Arrays.asList(Expression.class, Method.class, Expression[].class));
                ListBuffer<JCTree.JCExpression> of = new ListBuffer<>();

                Symbol symbol;
                if (methodSelect instanceof JCTree.JCIdent)
                {
                    JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
                    symbol = select.sym.location();
                    Symbol.MethodSymbol methodSymbol = getMethodSymbol(
                            symbol,
                            names.fromString(select.getName().toString()),
                            methodSelect.type.asMethodType()
                    );
                    if (methodSymbol.isStatic())
                    {
                        of.append(
                                treeMaker.App(
                                        getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                                        List.of(treeMaker.ClassLiteral(symbol.asType()))
                                )
                        );
                    }
                    else
                    {
                        of.append(
                                treeMaker.App(
                                        getFactoryMethod(Reference, Arrays.asList(Object.class, String.class)),
                                        List.of(treeMaker.This(thiz), treeMaker.Literal("this"))
                                )
                        );
                    }
                }
                else
                {
                    JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
                    symbol = select.sym.location();
                    of.append(deepMake(select.getExpression()));
                }

                Type.MethodType methodType = methodSelect.type.asMethodType();
                String methodName = methodSelect instanceof JCTree.JCFieldAccess
                        ? ((JCTree.JCFieldAccess) methodSelect).getIdentifier().toString()
                        : methodSelect.toString();

                of.append(
                                reflectMethod(
                                        symbol.asType(),
                                        methodName,
                                        ts
                                ))
                        .append(makeArray(Expression.class, args.toList()));

                Type type = symbol.asType();

                // todo：是否为扩展方法

                return treeMaker.App(
                        getFactoryMethod(
                                MethodCall,
                                argTypes
                        ),
                        of.toList()
                );

            }
            else if (tree instanceof JCTree.JCFieldAccess)
            {
                JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) tree;
                if (jcFieldAccess.sym.getKind() == ElementKind.FIELD
                        // class是关键字不能作为字段和函数名，可以直接判断
                        && jcFieldAccess.getIdentifier().toString().equals("class"))
                {
                    return treeMaker.App(
                            getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                            List.of(treeMaker.ClassLiteral(jcFieldAccess.getExpression().type))
                    );
                }
                else
                {
                    return treeMaker.App(
                            getFactoryMethod(FieldSelect, Arrays.asList(Expression.class, Field.class)),
                            List.of(
                                    deepMake(jcFieldAccess.getExpression()),
                                    reflectField(jcFieldAccess.getExpression().type, jcFieldAccess.getIdentifier().toString())
                            )
                    );
                }
            }
            else if (tree instanceof JCTree.JCParens)
            {
                JCTree.JCParens jcParens = (JCTree.JCParens) tree;
                JCTree.JCExpression expr = deepMake(jcParens.getExpression());
                return treeMaker.App(
                        getFactoryMethod(Parens, Collections.singletonList(Expression.class)),
                        List.of(expr)
                );
            }
            else if (tree instanceof JCTree.JCBlock)
            {
                JCTree.JCBlock jcBlock = (JCTree.JCBlock) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCStatement statement : jcBlock.getStatements())
                {
                    args.append(deepMake(statement));
                }
                return treeMaker.App(
                        getFactoryMethod(Block, Arrays.asList(Expression[].class, boolean.class)),
                        List.of(
                                makeArray(Expression.class, args.toList()),
                                treeMaker.Literal(jcBlock.isStatic())
                        )
                );
            }
            else if (tree instanceof JCTree.JCExpressionStatement)
            {
                JCTree.JCExpressionStatement jcExpressionStatement = (JCTree.JCExpressionStatement) tree;
                return deepMake(jcExpressionStatement.getExpression());
            }
            else if (tree instanceof JCTree.JCUnary)
            {
                JCTree.JCUnary jcUnary = (JCTree.JCUnary) tree;
                JCTree.JCExpression expr = deepMake(jcUnary.getExpression());
                return treeMaker.App(
                        getFactoryMethod(Unary, Arrays.asList(Expression.class, OperatorType.class)),
                        List.of(expr, getOperator(jcUnary.getTag()))
                );
            }
            else if (tree instanceof JCTree.JCAssign)
            {
                JCTree.JCAssign jcAssign = (JCTree.JCAssign) tree;
                JCTree.JCExpression left = deepMake(jcAssign.getVariable());
                JCTree.JCExpression right = deepMake(jcAssign.getExpression());
                return treeMaker.App(
                        getFactoryMethod(Assign, Arrays.asList(Expression.class, Expression.class)),
                        List.of(left, right)
                );
            }
            else if (tree instanceof JCTree.JCAssignOp)
            {
                JCTree.JCAssignOp jcAssignOp = (JCTree.JCAssignOp) tree;
                JCTree.JCExpression left = deepMake(jcAssignOp.getVariable());
                JCTree.JCExpression right = deepMake(jcAssignOp.getExpression());
                return treeMaker.App(
                        getFactoryMethod(AssignOp, Arrays.asList(Expression.class, Expression.class, OperatorType.class)),
                        List.of(left, right, getOperator(jcAssignOp.getTag()))
                );
            }
            else if (tree instanceof JCTree.JCVariableDecl)
            {
                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                JCTree.JCVariableDecl localVar = getLocalVar(jcVariableDecl.type, jcVariableDecl.getName().toString());
                jcStatements.append(localVar);
                tempVariableDeclMap.put(jcVariableDecl.getName(), localVar);
                args.append(treeMaker.Ident(localVar));
                if (jcVariableDecl.getInitializer() != null)
                {
                    args.append(deepMake(jcVariableDecl.getInitializer()));
                }
                else
                {
                    args.append(getNull());
                }
                return treeMaker.App(
                        getFactoryMethod(Variable, Arrays.asList(ParameterExpression.class, Expression.class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCArrayAccess)
            {
                JCTree.JCArrayAccess jcArrayAccess = (JCTree.JCArrayAccess) tree;
                JCTree.JCExpression indexed = deepMake(jcArrayAccess.getExpression());
                JCTree.JCExpression index = deepMake(jcArrayAccess.getIndex());
                return treeMaker.App(
                        getFactoryMethod(Index, Arrays.asList(Expression.class, Expression.class)),
                        List.of(indexed, index)
                );
            }
            else if (tree instanceof JCTree.JCNewClass)
            {
                JCTree.JCNewClass jcNewClass = (JCTree.JCNewClass) tree;
                java.util.List<Class<?>> classes = new ArrayList<>(4);
                ListBuffer<JCTree.JCExpression> all = new ListBuffer<>();

                //class
                classes.add(Class.class);
                all.append(treeMaker.ClassLiteral(jcNewClass.type));

                //typeArg
                ListBuffer<JCTree.JCExpression> typeArgs = new ListBuffer<>();
                if (jcNewClass.getIdentifier() instanceof JCTree.JCTypeApply)
                {
                    JCTree.JCTypeApply typeApply = (JCTree.JCTypeApply) jcNewClass.getIdentifier();
                    for (JCTree.JCExpression typeArgument : typeApply.getTypeArguments())
                    {
                        typeArgs.append(treeMaker.ClassLiteral(typeArgument.type));
                    }
                }
                classes.add(Class[].class);
                all.append(makeArray(Class.class, typeArgs.toList()));

                //arg
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression argument : jcNewClass.getArguments())
                {
                    args.append(deepMake(argument));
                }
                classes.add(Expression[].class);
                all.append(makeArray(Expression.class, args.toList()));

                //body
                JCTree.JCClassDecl classBody = jcNewClass.getClassBody();
                classes.add(BlockExpression.class);
                if (classBody != null)
                {
                    ListBuffer<JCTree.JCExpression> body = new ListBuffer<>();
                    //todo:目前只记录字段定义
                    for (JCTree member : classBody.getMembers())
                    {
                        if (!(member instanceof JCTree.JCVariableDecl)) continue;
                        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) member;
                        JCTree.JCExpression variable = deepMake(variableDecl);
                        body.add(variable);
                    }
                    all.append(treeMaker.App(
                            getFactoryMethod(Block, Collections.singletonList(Expression[].class)),
                            List.of(makeArray(Expression.class, body.toList()))
                    ));
                }
                else
                {
                    all.append(getNull());
                }

                return treeMaker.App(
                        getFactoryMethod(New, classes),
                        all.toList()
                );
            }
            else if (tree instanceof JCTree.JCNewArray)
            {
                JCTree.JCNewArray jcNewArray = (JCTree.JCNewArray) tree;
                ListBuffer<JCTree.JCExpression> dims = new ListBuffer<>();
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression dimension : jcNewArray.getDimensions())
                {
                    dims.append(deepMake(dimension));
                }
                for (JCTree.JCExpression initializer : jcNewArray.getInitializers())
                {
                    args.append(deepMake(initializer));
                }
                return treeMaker.App(
                        getFactoryMethod(NewArray, Arrays.asList(Class.class, Expression[].class, Expression[].class)),
                        List.of(
                                treeMaker.ClassLiteral(jcNewArray.getType().type),
                                makeArray(Expression.class, dims.toList()),
                                makeArray(Expression.class, args.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCReturn)
            {
                JCTree.JCReturn jcReturn = (JCTree.JCReturn) tree;
                JCTree.JCExpression result = deepMake(jcReturn.getExpression());
                return treeMaker.App(
                        getFactoryMethod(Return, Collections.singletonList(Expression.class)),
                        List.of(result)
                );
            }
            else if (tree instanceof JCTree.JCBreak)
            {
                return treeMaker.App(getFactoryMethod(Break, Collections.emptyList()));
            }
            else if (tree instanceof JCTree.JCContinue)
            {
                return treeMaker.App(getFactoryMethod(Continue, Collections.emptyList()));
            }
            else if (tree instanceof JCTree.JCConditional)
            {
                JCTree.JCConditional jcConditional = (JCTree.JCConditional) tree;
                JCTree.JCExpression cond = deepMake(jcConditional.getCondition());
                JCTree.JCExpression ifTrue = deepMake(jcConditional.getTrueExpression());
                JCTree.JCExpression ifFalse = deepMake(jcConditional.getFalseExpression());
                return treeMaker.App(
                        getFactoryMethod(Conditional, Arrays.asList(Expression.class, Expression.class, Expression.class)),
                        List.of(cond, ifTrue, ifFalse)
                );
            }
            else if (tree instanceof JCTree.JCIf)
            {
                JCTree.JCIf jcIf = (JCTree.JCIf) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                JCTree.JCExpression cond = deepMake(jcIf.getCondition());
                args.append(cond);
                if (jcIf.getThenStatement() != null)
                {
                    JCTree.JCExpression then = deepMake(jcIf.getThenStatement());
                    args.append(then);
                }
                else
                {
                    args.append(getNull());
                }
                if (jcIf.getElseStatement() != null)
                {
                    JCTree.JCExpression elSe = deepMake(jcIf.getElseStatement());
                    args.append(elSe);
                }
                else
                {
                    args.append(getNull());
                }
                return treeMaker.App(
                        getFactoryMethod(If, Arrays.asList(Expression.class, Expression.class, Expression.class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCForLoop)
            {
                JCTree.JCForLoop jcForLoop = (JCTree.JCForLoop) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                ListBuffer<JCTree.JCExpression> inits = new ListBuffer<>();
                for (JCTree.JCStatement jcStatement : jcForLoop.getInitializer())
                {
                    inits.append(deepMake(jcStatement));
                }
                args.append(makeArray(Expression.class, inits.toList()));
                if (jcForLoop.getCondition() != null)
                {
                    args.append(deepMake(jcForLoop.getCondition()));
                }
                else
                {
                    args.append(getNull());
                }
                ListBuffer<JCTree.JCExpression> steps = new ListBuffer<>();
                for (JCTree.JCExpressionStatement expressionStatement : jcForLoop.getUpdate())
                {
                    steps.append(deepMake(expressionStatement));
                }
                args.append(makeArray(Expression.class, steps.toList()));
                if (jcForLoop.getStatement() != null)
                {
                    args.append(deepMake(jcForLoop.getStatement()));
                }
                else
                {
                    args.append(getNull());
                }
                return treeMaker.App(
                        getFactoryMethod(For, Arrays.asList(Expression[].class, Expression.class, Expression[].class, Expression.class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCEnhancedForLoop)
            {
                JCTree.JCEnhancedForLoop jcEnhancedForLoop = (JCTree.JCEnhancedForLoop) tree;
                JCTree.JCExpression var = deepMake(jcEnhancedForLoop.getVariable());
                JCTree.JCExpression expr = deepMake(jcEnhancedForLoop.getExpression());
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(var).append(expr);
                if (jcEnhancedForLoop.getStatement() != null)
                {
                    args.append(deepMake(jcEnhancedForLoop.getStatement()));
                }
                else
                {
                    args.append(getNull());
                }
                return treeMaker.App(
                        getFactoryMethod(Foreach, Arrays.asList(VariableExpression.class, Expression.class, Expression.class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCWhileLoop)
            {
                JCTree.JCWhileLoop jcWhileLoop = (JCTree.JCWhileLoop) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(deepMake(jcWhileLoop.getCondition()));
                if (jcWhileLoop.getStatement() != null)
                {
                    args.append(deepMake(jcWhileLoop.getStatement()));
                }
                else
                {
                    args.append(getNull());
                }
                return treeMaker.App(
                        getFactoryMethod(While, Arrays.asList(Expression.class, Expression.class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCSwitch)
            {
                JCTree.JCSwitch jcSwitch = (JCTree.JCSwitch) tree;
                JCTree.JCExpression selector = deepMake(jcSwitch.getExpression());
                ListBuffer<JCTree.JCExpression> cases = new ListBuffer<>();
                for (JCTree.JCCase aCase : jcSwitch.getCases())
                {
                    cases.append(deepMake(aCase));
                }
                return treeMaker.App(
                        getFactoryMethod(Switch, Arrays.asList(Expression.class, CaseExpression[].class)),
                        List.of(
                                selector,
                                makeArray(CaseExpression.class, cases.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCCase)
            {
                JCTree.JCCase jcCase = (JCTree.JCCase) tree;
                JCTree.JCExpression part = deepMake(jcCase.getExpression());
                ListBuffer<JCTree.JCExpression> stats = new ListBuffer<>();
                for (JCTree.JCStatement statement : jcCase.getStatements())
                {
                    stats.append(deepMake(statement));
                }
                return treeMaker.App(
                        getFactoryMethod(Case, Arrays.asList(Expression.class, Expression[].class)),
                        List.of(
                                part,
                                makeArray(Expression.class, stats.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCTry)
            {
                JCTree.JCTry jcTry = (JCTree.JCTry) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(deepMake(jcTry.getBlock()));
                ListBuffer<JCTree.JCExpression> catches = new ListBuffer<>();
                for (JCTree.JCCatch aCatch : jcTry.getCatches())
                {
                    catches.append(deepMake(aCatch));
                }
                args.append(makeArray(CatchExpression.class, catches.toList()));
                if (jcTry.getFinallyBlock() != null)
                {
                    args.append(deepMake(jcTry.getFinallyBlock()));
                }
                else
                {
                    args.append(getNull());
                }
                ListBuffer<JCTree.JCExpression> resources = new ListBuffer<>();
                for (JCTree resource : jcTry.getResources())
                {
                    resources.append(deepMake(resource));
                }
                args.append(makeArray(Expression.class, resources.toList()));
                return treeMaker.App(
                        getFactoryMethod(Try, Arrays.asList(BlockExpression.class, CatchExpression[].class, BlockExpression.class, Expression[].class)),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCCatch)
            {
                JCTree.JCCatch jcCatch = (JCTree.JCCatch) tree;
                JCTree.JCExpression param = deepMake(jcCatch.getParameter());
                JCTree.JCExpression body = deepMake(jcCatch.getBlock());
                return treeMaker.App(
                        getFactoryMethod(Catch, Arrays.asList(VariableExpression.class, BlockExpression.class)),
                        List.of(param, body)
                );
            }
            else if (tree instanceof JCTree.JCThrow)
            {
                JCTree.JCThrow jcThrow = (JCTree.JCThrow) tree;
                JCTree.JCExpression expr = deepMake(jcThrow.getExpression());
                return treeMaker.App(
                        getFactoryMethod(Throw, Collections.singletonList(Expression.class)),
                        List.of(expr)
                );
            }
            else if (tree instanceof JCTree.JCTypeCast)
            {
                JCTree.JCTypeCast jcTypeCast = (JCTree.JCTypeCast) tree;
                JCTree.JCExpression target = deepMake(jcTypeCast.getType());
                JCTree.JCExpression expr = deepMake(jcTypeCast.getExpression());
                return treeMaker.App(
                        getFactoryMethod(TypeCast, Arrays.asList(Class.class, Expression.class)),
                        List.of(target, expr)
                );
            }
            else if (tree instanceof JCTree.JCLambda)
            {
                JCTree.JCLambda lambda = (JCTree.JCLambda) tree;
                JCTree.JCVariableDecl localLambdaExpr;
                if (tempLambdaMap.containsKey(lambda))
                {
                    localLambdaExpr = tempLambdaMap.get(lambda);
                }
                else
                {
                    Type.MethodType methodType = (Type.MethodType) types.findDescriptorType(lambda.type);
                    Type returnType = methodType.getReturnType();
                    ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                    for (VariableTree variableTree : lambda.getParameters())
                    {
                        JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) variableTree;
                        JCTree.JCVariableDecl localVar = getLocalVar(jcVariableDecl.type, jcVariableDecl.getName().toString());
                        args.append(treeMaker.Ident(localVar));
                        variableDeclMap.put(jcVariableDecl.getName(), localVar);
                        jcStatements.append(localVar);
                    }
                    JCTree.JCExpression body = deepMake(lambda.getBody());
//                    System.out.println(lambda.type);
//                    System.out.println(returnType);
                    localLambdaExpr = getLocalLambdaExpr(
                            body,
                            args,
                            returnType,
                            lambda.type
                    );
                    jcStatements.append(localLambdaExpr);
                    tempLambdaMap.put(lambda, localLambdaExpr);
                }
                return treeMaker.Ident(localLambdaExpr);
            }
            throw new RuntimeException("不支持的类型:" + tree.type + "\n" + tree);
        }
    }
}
