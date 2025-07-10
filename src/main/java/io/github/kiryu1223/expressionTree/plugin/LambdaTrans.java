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

import static io.github.kiryu1223.expressionTree.expressions.Kind.*;

public class LambdaTrans extends TreeTranslator
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Symtab symtab;
    private final ClassReader classReader;
    private final Object moduleSymbol;
    private final ArrayDeque<Symbol> thizDeque = new ArrayDeque<>();
    private final ArrayDeque<Symbol> ownerDeque = new ArrayDeque<>();
    private final ArrayDeque<Symbol.VarSymbol> varSymbolDeque = new ArrayDeque<>();
    private final ArrayDeque<ListBuffer<JCTree.JCStatement>> statementsDeque = new ArrayDeque<>();
    private int parameterIndex = 0;

    public LambdaTrans(TreeMaker treeMaker, Types types, Names names, Symtab symtab, ClassReader classReader, Object moduleSymbol)
    {
        this.treeMaker = treeMaker;
        this.types = types;
        this.names = names;
        this.symtab = symtab;
        this.classReader = classReader;
        this.moduleSymbol = moduleSymbol;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree)
    {
        thizDeque.push(tree.sym);
        super.visitClassDef(tree);
        thizDeque.pop();
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree)
    {
        ownerDeque.push(tree.sym);
        super.visitMethodDef(tree);
        ownerDeque.pop();
    }

    @Override
    public void visitBlock(JCTree.JCBlock tree)
    {
        if (!tree.isStatic() && !ownerDeque.isEmpty())
        {
            ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
            statementsDeque.push(statements);
            for (JCTree.JCStatement stat : tree.stats)
            {
                statements.add(translate(stat));
            }
            statementsDeque.pop();
            tree.stats = statements.toList();
            result = tree;
        }
        else
        {
            super.visitBlock(tree);
        }
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree)
    {
        tree.meth = translate(tree.getMethodSelect());
        Symbol.MethodSymbol methodSymbol = methodInvocationGetMethodSymbol(tree);
        if (methodSymbol.isVarArgs()) {
            for (Symbol.VarSymbol parameter : methodSymbol.getParameters()) {
                if (parameter.getAnnotation(Expr.class)!=null) {
                    throw new RuntimeException("表达式树暂不支持变长参数:"+tree);
                }
            }
            tree.args = translate(tree.args);
            result = tree;
        }
        else {
            List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            List<JCTree.JCExpression> jcExpressions = tree.getArguments();
            ListBuffer<Type> argsType = new ListBuffer<>();
            booleanDeque.push(new boolean[]{false});
            for (int i = 0; i < jcExpressions.size(); i++)
            {
                Symbol.VarSymbol varSymbol = parameters.get(i);
//            System.out.println(Flags.asFlagSet(varSymbol.flags())+" "+varSymbol.asType()+" "+varSymbol.getSimpleName());
                varSymbolDeque.push(varSymbol);
                JCTree.JCExpression arg = jcExpressions.get(i);
                checkIsLambda(methodSymbol, varSymbol, arg);
                JCTree.JCExpression translate = translate(arg);
                args.add(translate);
                varSymbolDeque.pop();

                //泛型参数会有类型问题
                if (varSymbol.getAnnotation(Expr.class) == null) {
                    argsType.add(varSymbol.asType());
                }
                else {
                    argsType.add(translate.type);
                }
            }
            if (booleanDeque.pop()[0])
            {
                Symbol.MethodSymbol targetMethodSymbol = getTargetMethodSymbol(methodSymbol, argsType);
                trySetMethodSymbol(tree, targetMethodSymbol);
                tree.args = args.toList();
            }
            result = tree;
        }
    }

    private final Deque<boolean[]> booleanDeque = new ArrayDeque<>();

    // 把需要转换的lambda变成包装后的形式
    @Override
    public void visitLambda(JCTree.JCLambda tree)
    {
        if (varSymbolDeque.isEmpty())
        {
            super.visitLambda(tree);
        }
        else
        {
            Symbol.VarSymbol varSymbol = varSymbolDeque.peek();
            Expr expr = varSymbol.getAnnotation(Expr.class);
            if (expr != null)
            {
                checkBody(expr.value(), tree.getBodyKind(), tree);
                JCTree.JCExpression expression = deepMake(tree);
                Symbol.MethodSymbol exprSymbol = getMethodSymbol(ExprTree.class, "Expr", Arrays.asList(Delegate.class, LambdaExpression.class));
                JCTree.JCExpression fa = refMakeSelector(treeMaker.Ident(getClassSymbol(ExprTree.class)), exprSymbol);
                result = treeMaker.App(fa, List.of(tree, expression));
                booleanDeque.peek()[0] = true;
            }
            else
            {
                super.visitLambda(tree);
            }
        }
    }

    private final Map<Name, JCTree.JCVariableDecl> lambdaVarMap = new HashMap<>();

    private boolean inNewClassBlock=false;

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
            if (jcIdent.sym.getKind().isClass() || jcIdent.sym.getKind().isInterface())
            {
                return treeMaker.App(
                        getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                        List.of(treeMaker.ClassLiteral(jcIdent.type))
                );
            }
            else if (lambdaVarMap.containsKey(jcIdent.getName()))
            {
                JCTree.JCVariableDecl jcVariableDecl = lambdaVarMap.get(jcIdent.getName());
                return treeMaker.Ident(jcVariableDecl);
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
            JCTree.JCMethodInvocation invocation = (JCTree.JCMethodInvocation) tree;
            ListBuffer<JCTree.JCExpression> of = new ListBuffer<>();
            JCTree.JCExpression methodSelect = invocation.getMethodSelect();
            Symbol.MethodSymbol methodSymbol = methodInvocationGetMethodSymbol(invocation);
            List<JCTree.JCExpression> jcExpressions = invocation.getArguments();
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            for (JCTree.JCExpression arg : jcExpressions)
            {
                args.add(deepMake(arg));
            }
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
                of.append(deepMake(select.getExpression()));
            }
            else if (methodSelect instanceof JCTree.JCIdent)
            {
                JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
                if (methodSymbol.isStatic())
                {
                    of.append(
                            treeMaker.App(
                                    getFactoryMethod(StaticClass, Collections.singletonList(Class.class)),
                                    List.of(treeMaker.ClassLiteral(methodSymbol.location().asType()))
                            )
                    );
                }
                else
                {
                    of.append(
                            inNewClassBlock?getNull():treeMaker.App(
                                    getFactoryMethod(Reference, Arrays.asList(Object.class, String.class)),
                                    List.of(treeMaker.This(thizDeque.peek().type), treeMaker.Literal("this"))
                            )
                    );
                }
            }
            else
            {
                throw new RuntimeException("意料之外的的表达式:" + tree);
            }

            of.append(reflectMethod(methodSymbol.location().asType(), methodName, ts))
                    .append(makeArray(Expression.class, args.toList()));

            return treeMaker.App(
                    getFactoryMethod(
                            MethodCall,
                            Arrays.asList(Expression.class, Method.class, Expression[].class)
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
            ListBuffer<Name> locals = new ListBuffer<>();
            for (JCTree.JCStatement statement : jcBlock.getStatements())
            {
                if (statement instanceof JCTree.JCVariableDecl)
                {
                    JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) statement;
                    JCTree.JCVariableDecl localVar = getLocalVar(jcVariableDecl.type, jcVariableDecl.getName().toString());
                    locals.add(jcVariableDecl.getName());
                    lambdaVarMap.put(jcVariableDecl.getName(), localVar);
                    statementsDeque.peek().add(localVar);
                }
                args.append(deepMake(statement));
            }
            for (Name local : locals)
            {
                lambdaVarMap.remove(local);
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
            ListBuffer<JCTree.JCStatement> statements = statementsDeque.peek();
            Name name = jcVariableDecl.getName();
            JCTree.JCVariableDecl localVar = getLocalVar(tree.type, name.toString());
            lambdaVarMap.put(name, localVar);
            statements.add(localVar);
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            args.append(treeMaker.Ident(localVar));
            jcVariableDecl.getInitializer();
            JCTree.JCExpression initializer = jcVariableDecl.getInitializer();
            if (initializer != null)
            {
                args.append(deepMake(initializer));
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
            //constructor
            classes.add(Constructor.class);
            Symbol.MethodSymbol init = (Symbol.MethodSymbol) jcNewClass.constructor;
            ListBuffer<JCTree.JCExpression> types = new ListBuffer<>();
            for (Symbol.VarSymbol parameter : init.getParameters())
            {
                types.add(treeMaker.ClassLiteral(parameter.asType()));
            }
            all.append(reflectConstructor(jcNewClass.type, types));
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
                // 只记录字段
                List<JCTree> members = classBody.getMembers();
                ListBuffer<Name> local = new ListBuffer<>();
                for (JCTree member : members)
                {
                    if (member instanceof JCTree.JCVariableDecl) {
                        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) member;
                        local.add(variableDecl.getName());
                        JCTree.JCExpression variable = deepMake(variableDecl);
                        body.add(variable);
                    }
                    else if (member instanceof JCTree.JCBlock) {
                        inNewClassBlock=true;
                        body.add(deepMake(member));
                        inNewClassBlock=false;
                    }
                }
                for (Name name : local)
                {
                    lambdaVarMap.remove(name);
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
            JCTree.JCMethodInvocation app = treeMaker.App(
                    getFactoryMethod(New, classes),
                    all.toList()
            );
            return app;
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
            return treeMaker.App(
                    getFactoryMethod(Return, Collections.singletonList(Expression.class)),
                    List.of(deepMake(jcReturn.getExpression()))
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
                    getFactoryMethod(TypeCast, Arrays.asList(StaticClassExpression.class, Expression.class)),
                    List.of(target, expr)
            );
        }
        else if (tree instanceof JCTree.JCLambda)
        {
            JCTree.JCLambda lambda = (JCTree.JCLambda) tree;
            ListBuffer<JCTree.JCStatement> block = statementsDeque.peek();
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            for (JCTree.JCVariableDecl param : lambda.params)
            {
                // 为每个lambda入参创建一个局部变量
                JCTree.JCVariableDecl localVar = getLocalVar(param.type, param.getName().toString());
                // 添加到当前代码块
                block.add(localVar);
                // 以入参名作为key
                lambdaVarMap.put(param.name, localVar);
                args.add(treeMaker.Ident(localVar));
            }
            JCTree.JCExpression expression = deepMake(lambda.getBody());
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
            block.add(localLambdaExpr);
            // 归还lambda参数
            for (JCTree.JCVariableDecl param : lambda.params)
            {
                lambdaVarMap.remove(param.name);
            }
            return treeMaker.Ident(localLambdaExpr);
        }
        throw new RuntimeException("不支持的类型:" + tree.type + "\n" + tree);
    }

    // region [Util]

    private void checkIsLambda(Symbol.MethodSymbol methodSymbol, Symbol.VarSymbol varSymbol, JCTree.JCExpression arg)
    {
        if (varSymbol.getAnnotation(Expr.class) != null && !(arg instanceof JCTree.JCLambda))
        {
            throw new RuntimeException(String.format("%s函数的%s参数要求入参为lambda表达式，但是收到了%s", methodSymbol, varSymbol, arg));
        }
    }

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
        return "lambdaParameter_" + parameterIndex++;
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
                Type erasure = types.erasure(parameter.asType());
                varTypes.add(erasure);
            }
            java.util.List<Type> argTypes = new ArrayList<>();
            for (Type type : argsType)
            {
                Type erasure = types.erasure(type);
                argTypes.add(erasure);
            }
            boolean subtypes = typesEqual(varTypes, argTypes);
            if (subtypes)
            {
                return element;
            }
        }
        throw new RuntimeException(methodSymbol + " " + argsType);
    }

    private void trySetMethodSymbol(JCTree.JCMethodInvocation tree, Symbol.MethodSymbol methodSymbol)
    {
        JCTree.JCExpression methodSelect = tree.getMethodSelect();
        tree.setType(methodSymbol.getReturnType());
        if (methodSelect instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
            tree.meth = refMakeSelector(select.getExpression(), methodSymbol);
        }
        else
        {
            JCTree.JCIdent select = (JCTree.JCIdent) methodSelect;
            tree.meth = treeMaker.Ident(methodSymbol);
        }
    }

    // endregion
}
