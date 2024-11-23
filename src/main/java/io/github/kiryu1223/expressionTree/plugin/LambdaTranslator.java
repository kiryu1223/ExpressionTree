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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
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
    private final Map<Name, JCTree.JCVariableDecl> lambdaVarMap = new HashMap<>();
    private final Map<JCTree.JCLambda, JCTree.JCVariableDecl> lambdaCache = new HashMap<>();
    private final ArrayDeque<ListBuffer<Name>> argNameDeque = new ArrayDeque<>();

    public LambdaTranslator(TreeMaker treeMaker, Types types, Names names, Symtab symtab, ClassReader classReader, Object moduleSymbol, ArrayDeque<Symbol> thizDeque, ArrayDeque<Symbol> ownerDeque, ArrayDeque<Symbol.VarSymbol> varSymbolDeque, ArrayDeque<ListBuffer<JCTree.JCStatement>> statementsDeque, AtomicInteger argIndex)
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
    }

    public JCTree.JCExpression translateToExprTree(JCTree.JCLambda tree)
    {
        JCTree.JCExpression expression = translateV(tree);
        Symbol.MethodSymbol exprSymbol = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
        JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), exprSymbol);
        return treeMaker.App(fa, List.of(tree, expression));
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

    // 方法调用
    @Override
    public void visitApply(JCTree.JCMethodInvocation tree)
    {
        ListBuffer<JCTree.JCExpression> of = new ListBuffer<>();
        JCTree.JCExpression methodSelect = tree.getMethodSelect();

        Symbol.MethodSymbol methodSymbol = methodInvocationGetMethodSymbol(tree);
        List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();
        List<JCTree.JCExpression> jcExpressions = tree.getArguments();
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        ListBuffer<JCTree.JCExpression> newCode = new ListBuffer<>();
        boolean changed = false;
        ListBuffer<Type> argsType = new ListBuffer<>();
        for (int i = 0; i < jcExpressions.size(); i++)
        {
            JCTree.JCExpression arg = jcExpressions.get(i);
            varSymbolDeque.push(parameters.get(i));
            JCTree.JCExpression jcExpression = translateV(arg);
            args.add(jcExpression);
            if (arg instanceof JCTree.JCLambda && lambdaCache.containsKey(arg))
            {
                JCTree.JCLambda jcLambda = (JCTree.JCLambda) arg;
                Symbol.MethodSymbol exprSymbol = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
                JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), exprSymbol);
                JCTree.JCMethodInvocation apply = treeMaker.App(fa, List.of(jcLambda, jcExpression));
                newCode.add(apply);
                changed = true;
                argsType.add(apply.type);
            }
            else
            {
                newCode.add(arg);
                argsType.add(arg.type);
            }
            varSymbolDeque.pop();
        }
        if (changed)
        {
            Symbol.MethodSymbol targetMethodSymbol = getTargetMethodSymbol(methodSymbol, argsType);
            trySetMethodSymbol(tree, targetMethodSymbol);
        }
        tree.args = newCode.toList();
        // 获取方法名
        String methodName = methodSelect instanceof JCTree.JCFieldAccess
                ? ((JCTree.JCFieldAccess) methodSelect).getIdentifier().toString()
                : methodSelect.toString();

        // 获取所有参数的类型
        ListBuffer<JCTree.JCExpression> ts = new ListBuffer<>();
        for (Type parameterType : methodSelect.type.asMethodType().getParameterTypes())
        {
            ts.add(treeMaker.ClassLiteral(parameterType));
        }

        if (methodSelect instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
            of.append(translateV(select.getExpression()));
        }
        else if (methodSelect instanceof JCTree.JCIdent)
        {
            JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
            of.append(translateV(select));
        }
        else
        {
            throw new RuntimeException("意料之外的的表达式:" + tree);
        }

        of.append(reflectMethod(methodSymbol.location().asType(), methodName, ts))
                .append(makeArray(Expression.class, args.toList()));

        result = treeMaker.App(
                getFactoryMethod(
                        MethodCall,
                        Arrays.asList(Expression.class, Method.class, Expression[].class)
                ),
                of.toList()
        );
    }

    // lambda
    @Override
    public void visitLambda(JCTree.JCLambda tree)
    {
        Symbol.VarSymbol varSymbol = varSymbolDeque.peek();
        // 检查是否需要进行表达式树展开
        if (varSymbol.getAnnotation(Expr.class) == null)
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
            // 拿到当前代码块
            ListBuffer<JCTree.JCStatement> peek = statementsDeque.peek();
            // 收集lambda参数
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            for (JCTree.JCVariableDecl param : tree.params)
            {
                // 为每个lambda入参创建一个局部变量
                JCTree.JCVariableDecl localVar = getLocalVar(param.type, param.getName().toString());
                // 添加到当前代码块
                peek.add(localVar);
                // 以入参名作为key
                lambdaVarMap.put(param.name, localVar);
                args.add(treeMaker.Ident(localVar));
            }
            // 解析
            JCTree.JCExpression expression = translateV(tree.body);
            // 获取lambda返回类型
            Type.MethodType methodType = types.findDescriptorType(tree.type).asMethodType();
            Type returnType = methodType.getReturnType();
            // 创建一个表达式树的局部变量
            JCTree.JCVariableDecl localLambdaExpr = getLocalLambdaExpr(
                    expression,
                    args,
                    returnType,
                    tree.type
            );
            // 添加到当前代码块
            peek.add(localLambdaExpr);
            // 缓存lambda表达式，后续用到
            lambdaCache.put(tree, localLambdaExpr);
            JCTree.JCExpression ident = treeMaker.Ident(localLambdaExpr);

//            Symbol.MethodSymbol exprSymbol = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
//            JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), exprSymbol);
//            JCTree.JCMethodInvocation apply = treeMaker.App(fa, List.of(tree, ident));
            result = ident;

            // 归还lambda参数
            for (JCTree.JCVariableDecl param : tree.params)
            {
                lambdaVarMap.remove(param.name);
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
        ListBuffer<JCTree.JCStatement> statements = statementsDeque.peek();
        ListBuffer<Name> peek = argNameDeque.peek();
        Name name = tree.getName();
        JCTree.JCVariableDecl localVar = getLocalVar(tree.type, name.toString());
        peek.add(name);
        lambdaVarMap.put(name, localVar);
        statements.add(localVar);

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

    // 二元运算
    @Override
    public void visitBinary(JCTree.JCBinary tree)
    {
        JCTree.JCExpression left = translateV(tree.getLeftOperand());
        JCTree.JCExpression right = translateV(tree.getRightOperand());
        result = treeMaker.App(
                getFactoryMethod(Binary, Arrays.asList(Expression.class, Expression.class, OperatorType.class)),
                List.of(left, right, getOperator(tree.getTag()))
        );
    }

    // 一元运算
    @Override
    public void visitUnary(JCTree.JCUnary tree)
    {
        JCTree.JCExpression expr = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(Unary, Arrays.asList(Expression.class, OperatorType.class)),
                List.of(expr, getOperator(tree.getTag()))
        );
    }

    // 执行
    @Override
    public void visitExec(JCTree.JCExpressionStatement tree)
    {
        result = translateV(tree.getExpression());
    }

    // 三元表达式
    @Override
    public void visitConditional(JCTree.JCConditional tree)
    {
        JCTree.JCExpression cond = translateV(tree.getCondition());
        JCTree.JCExpression ifTrue = translateV(tree.getTrueExpression());
        JCTree.JCExpression ifFalse = translateV(tree.getFalseExpression());
        result = treeMaker.App(
                getFactoryMethod(Conditional, Arrays.asList(Expression.class, Expression.class, Expression.class)),
                List.of(cond, ifTrue, ifFalse)
        );
    }

    // 赋值
    @Override
    public void visitAssign(JCTree.JCAssign tree)
    {
        JCTree.JCExpression left = translateV(tree.getVariable());
        JCTree.JCExpression right = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(Assign, Arrays.asList(Expression.class, Expression.class)),
                List.of(left, right)
        );
    }

    // 自运算
    @Override
    public void visitAssignop(JCTree.JCAssignOp tree)
    {
        JCTree.JCExpression left = translateV(tree.getVariable());
        JCTree.JCExpression right = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(AssignOp, Arrays.asList(Expression.class, Expression.class, OperatorType.class)),
                List.of(left, right, getOperator(tree.getTag()))
        );
    }

    // If
    @Override
    public void visitIf(JCTree.JCIf tree)
    {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        JCTree.JCExpression cond = translateV(tree.getCondition());
        args.append(cond);
        if (tree.getThenStatement() != null)
        {
            JCTree.JCExpression then = translateV(tree.getThenStatement());
            args.append(then);
        }
        else
        {
            args.append(getNull());
        }
        if (tree.getElseStatement() != null)
        {
            JCTree.JCExpression elSe = translateV(tree.getElseStatement());
            args.append(elSe);
        }
        else
        {
            args.append(getNull());
        }
        result = treeMaker.App(
                getFactoryMethod(If, Arrays.asList(Expression.class, Expression.class, Expression.class)),
                args.toList()
        );
    }

    // 数组下标运算
    @Override
    public void visitIndexed(JCTree.JCArrayAccess tree)
    {
        JCTree.JCExpression indexed = translateV(tree.getExpression());
        JCTree.JCExpression index = translateV(tree.getIndex());
        result = treeMaker.App(
                getFactoryMethod(Index, Arrays.asList(Expression.class, Expression.class)),
                List.of(indexed, index)
        );
    }

    // new对象
    @Override
    public void visitNewClass(JCTree.JCNewClass tree)
    {
        java.util.List<Class<?>> classes = new ArrayList<>(4);
        ListBuffer<JCTree.JCExpression> all = new ListBuffer<>();

        //class
        classes.add(Class.class);
        all.append(treeMaker.ClassLiteral(tree.type));

        //typeArg
        ListBuffer<JCTree.JCExpression> typeArgs = new ListBuffer<>();
        if (tree.getIdentifier() instanceof JCTree.JCTypeApply)
        {
            JCTree.JCTypeApply typeApply = (JCTree.JCTypeApply) tree.getIdentifier();
            for (JCTree.JCExpression typeArgument : typeApply.getTypeArguments())
            {
                typeArgs.append(treeMaker.ClassLiteral(typeArgument.type));
            }
        }
        classes.add(Class[].class);
        all.append(makeArray(Class.class, typeArgs.toList()));

        //constructor
        classes.add(Constructor.class);
        Symbol.MethodSymbol init = (Symbol.MethodSymbol) tree.constructor;
        ListBuffer<JCTree.JCExpression> types = new ListBuffer<>();
        for (Symbol.VarSymbol parameter : init.getParameters())
        {
            types.add(treeMaker.ClassLiteral(parameter.asType()));
        }
        all.append(reflectConstructor(tree.type, types));

        //arg
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        for (JCTree.JCExpression argument : tree.getArguments())
        {
            args.append(translateV(argument));
        }
        classes.add(Expression[].class);
        all.append(makeArray(Expression.class, args.toList()));

        //body
        JCTree.JCClassDecl classBody = tree.getClassBody();
        classes.add(BlockExpression.class);
        if (classBody != null)
        {
            ListBuffer<JCTree.JCExpression> body = new ListBuffer<>();
            // 只记录字段
            for (JCTree member : classBody.getMembers())
            {
                if (!(member instanceof JCTree.JCVariableDecl)) continue;
                JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) member;
                JCTree.JCExpression variable = translateV(variableDecl);
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

        result = treeMaker.App(
                getFactoryMethod(New, classes),
                all.toList()
        );
    }

    // new数组
    @Override
    public void visitNewArray(JCTree.JCNewArray tree)
    {
        ListBuffer<JCTree.JCExpression> dims = new ListBuffer<>();
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        for (JCTree.JCExpression dimension : tree.getDimensions())
        {
            dims.append(translateV(dimension));
        }
        for (JCTree.JCExpression initializer : tree.getInitializers())
        {
            args.append(translateV(initializer));
        }
        result = treeMaker.App(
                getFactoryMethod(NewArray, Arrays.asList(Class.class, Expression[].class, Expression[].class)),
                List.of(
                        treeMaker.ClassLiteral(tree.getType().type),
                        makeArray(Expression.class, dims.toList()),
                        makeArray(Expression.class, args.toList())
                )
        );
    }

    // return表达式
    @Override
    public void visitReturn(JCTree.JCReturn tree)
    {
        JCTree.JCExpression rt = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(Return, Collections.singletonList(Expression.class)),
                List.of(rt)
        );
    }

    // 跳出语句
    @Override
    public void visitBreak(JCTree.JCBreak tree)
    {
        result = treeMaker.App(getFactoryMethod(Break, Collections.emptyList()));
    }

    // 跳转语句
    @Override
    public void visitContinue(JCTree.JCContinue tree)
    {
        result = treeMaker.App(getFactoryMethod(Continue, Collections.emptyList()));
    }

    // for循环
    @Override
    public void visitForLoop(JCTree.JCForLoop tree)
    {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        ListBuffer<JCTree.JCExpression> inits = new ListBuffer<>();
        for (JCTree.JCStatement jcStatement : tree.getInitializer())
        {
            inits.append(translateV(jcStatement));
        }
        args.append(makeArray(Expression.class, inits.toList()));
        if (tree.getCondition() != null)
        {
            args.append(translateV(tree.getCondition()));
        }
        else
        {
            args.append(getNull());
        }
        ListBuffer<JCTree.JCExpression> steps = new ListBuffer<>();
        for (JCTree.JCExpressionStatement expressionStatement : tree.getUpdate())
        {
            steps.append(translateV(expressionStatement));
        }
        args.append(makeArray(Expression.class, steps.toList()));
        if (tree.getStatement() != null)
        {
            args.append(translateV(tree.getStatement()));
        }
        else
        {
            args.append(getNull());
        }
        result = treeMaker.App(
                getFactoryMethod(For, Arrays.asList(Expression[].class, Expression.class, Expression[].class, Expression.class)),
                args.toList()
        );
    }

    // foreach循环
    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop tree)
    {
        JCTree.JCExpression var = translateV(tree.getVariable());
        JCTree.JCExpression expr = translateV(tree.getExpression());
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        args.append(var).append(expr);
        if (tree.getStatement() != null)
        {
            args.append(translateV(tree.getStatement()));
        }
        else
        {
            args.append(getNull());
        }
        result = treeMaker.App(
                getFactoryMethod(Foreach, Arrays.asList(VariableExpression.class, Expression.class, Expression.class)),
                args.toList()
        );
    }

    // while循环
    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop tree)
    {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        args.append(translateV(tree.getCondition()));
        if (tree.getStatement() != null)
        {
            args.append(translateV(tree.getStatement()));
        }
        else
        {
            args.append(getNull());
        }
        result = treeMaker.App(
                getFactoryMethod(While, Arrays.asList(Expression.class, Expression.class)),
                args.toList()
        );
    }

    // switch语句
    @Override
    public void visitSwitch(JCTree.JCSwitch tree)
    {
        JCTree.JCExpression selector = translateV(tree.getExpression());
        ListBuffer<JCTree.JCExpression> cases = new ListBuffer<>();
        for (JCTree.JCCase aCase : tree.getCases())
        {
            cases.append(translateV(aCase));
        }
        result = treeMaker.App(
                getFactoryMethod(Switch, Arrays.asList(Expression.class, CaseExpression[].class)),
                List.of(
                        selector,
                        makeArray(CaseExpression.class, cases.toList())
                )
        );
    }

    // case语句
    @Override
    public void visitCase(JCTree.JCCase tree)
    {
        JCTree.JCExpression part = translateV(tree.getExpression());
        ListBuffer<JCTree.JCExpression> stats = new ListBuffer<>();
        for (JCTree.JCStatement statement : tree.getStatements())
        {
            stats.append(translateV(statement));
        }
        result = treeMaker.App(
                getFactoryMethod(Case, Arrays.asList(Expression.class, Expression[].class)),
                List.of(
                        part,
                        makeArray(Expression.class, stats.toList())
                )
        );
    }

    // try语句
    @Override
    public void visitTry(JCTree.JCTry tree)
    {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        args.append(translateV(tree.getBlock()));
        ListBuffer<JCTree.JCExpression> catches = new ListBuffer<>();
        for (JCTree.JCCatch aCatch : tree.getCatches())
        {
            catches.append(translateV(aCatch));
        }
        args.append(makeArray(CatchExpression.class, catches.toList()));
        if (tree.getFinallyBlock() != null)
        {
            args.append(translateV(tree.getFinallyBlock()));
        }
        else
        {
            args.append(getNull());
        }
        ListBuffer<JCTree.JCExpression> resources = new ListBuffer<>();
        for (JCTree resource : tree.getResources())
        {
            resources.append(translateV(resource));
        }
        args.append(makeArray(Expression.class, resources.toList()));
        result = treeMaker.App(
                getFactoryMethod(Try, Arrays.asList(BlockExpression.class, CatchExpression[].class, BlockExpression.class, Expression[].class)),
                args.toList()
        );
    }

    // catch语句
    @Override
    public void visitCatch(JCTree.JCCatch tree)
    {
        JCTree.JCExpression param = translateV(tree.getParameter());
        JCTree.JCExpression body = translateV(tree.getBlock());
        result = treeMaker.App(
                getFactoryMethod(Catch, Arrays.asList(VariableExpression.class, BlockExpression.class)),
                List.of(param, body)
        );
    }

    // throw语句
    @Override
    public void visitThrow(JCTree.JCThrow tree)
    {
        JCTree.JCExpression expr = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(Throw, Collections.singletonList(Expression.class)),
                List.of(expr)
        );
    }

    // 强制类型转换
    @Override
    public void visitTypeCast(JCTree.JCTypeCast tree)
    {
        JCTree.JCExpression target = translateV(tree.getType());
        JCTree.JCExpression expr = translateV(tree.getExpression());
        result = treeMaker.App(
                getFactoryMethod(TypeCast, Arrays.asList(Class.class, Expression.class)),
                List.of(target, expr)
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
        JCTree.JCNewArray jcNewArray = treeMaker.NewArray(treeMaker.Ident(getClassSymbol(clazz)), List.nil(), args);
        jcNewArray.setType(types.makeArrayType(getType(clazz)));
        return jcNewArray;
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

    private boolean typesEqual(java.util.List<Type> left, java.util.List<Type> right)
    {
        if (left.size() != right.size())
        {
            return false;
        }
        for (int i = 0; i < left.size(); i++)
        {
            Type leftType = left.get(i);
            Type rightType = right.get(i);
            if (!leftType.toString().equals(rightType.toString()))
            {
                return false;
            }
        }
        return true;
    }

    private Symbol.MethodSymbol getTargetMethodSymbol(Symbol.MethodSymbol methodSymbol, ListBuffer<Type> argsType)
    {
        Symbol location = methodSymbol.location();
        for (Symbol enclosedElement : location.getEnclosedElements())
        {
            if (!(enclosedElement instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol element = (Symbol.MethodSymbol) enclosedElement;
            if (!element.getSimpleName().equals(methodSymbol.getSimpleName())) continue;
            if (element.getParameters().size() != methodSymbol.getParameters().size()) continue;
            java.util.List<Type> varTypes = new ArrayList<>();
            for (Symbol.VarSymbol parameter : element.getParameters())
            {
                varTypes.add(types.erasure(parameter.asType()));
            }
            java.util.List<Type> argTypes = new ArrayList<>();
            for (Type type : argsType)
            {
                argTypes.add(types.erasure(type));
            }
            boolean subtypes = typesEqual(varTypes, argTypes);
            if (subtypes)
            {
                return element;
            }
        }
        throw new RuntimeException();
    }

    private void trySetMethodSymbol(JCTree.JCMethodInvocation tree, Symbol.MethodSymbol methodSymbol)
    {
        JCTree.JCExpression methodSelect = tree.getMethodSelect();
        if (methodSelect instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
            select.sym = methodSymbol;
        }
        else
        {
            JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
            select.sym = methodSymbol;
        }
    }

    // endregion
}
