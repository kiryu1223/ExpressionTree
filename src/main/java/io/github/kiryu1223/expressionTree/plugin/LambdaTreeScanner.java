package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
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

public class LambdaTreeScanner extends TreeScanner
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Symtab symtab;
    private final ClassReader classReader;
    private final Object moduleSymbol;
    private final ArrayDeque<Symbol> thizDeque = new ArrayDeque<>();
    private final ArrayDeque<Symbol> ownerDeque = new ArrayDeque<>();

    public LambdaTreeScanner(TreeMaker treeMaker, Types types, Names names, Symtab symtab, ClassReader classReader, Object moduleSymbol)
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
            ListBuffer<JCTree.JCStatement> news = new ListBuffer<>();
            for (JCTree.JCStatement statement : tree.getStatements())
            {
                scan(statement);
                news.add(statement);
            }
            tree.stats = news.toList();
        }
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree)
    {
        Symbol.MethodSymbol methodSymbol = methodInvocationGetMethodSymbol(tree);
        List<Symbol.VarSymbol> varSymbols = methodSymbol.getParameters();
        List<JCTree.JCExpression> parameters = tree.getArguments();
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        for (int i = 0; i < varSymbols.size(); i++)
        {
            Symbol.VarSymbol varSymbol = varSymbols.get(i);
            JCTree.JCExpression parameter = parameters.get(i);
            Expr expr = varSymbol.getAnnotation(Expr.class);
            // 没注解说明不是
            if (expr == null)
            {
                args.add(parameter);
                continue;
            }
            // 检查是不是lambda表达式
            if (!(parameter instanceof JCTree.JCLambda))
            {
                throw new RuntimeException(String.format("%s的第%d个参数期望为lambda表达式，目前为%s", methodSymbol, i, parameter));
            }
            JCTree.JCLambda jcLambda = (JCTree.JCLambda) parameter;
            LambdaExpressionTree.BodyKind bodyKind = jcLambda.getBodyKind();
            // 检查表达式体约束
            if ((expr.value() == Expr.BodyType.Expr && bodyKind == LambdaExpressionTree.BodyKind.STATEMENT)
                    || (expr.value() == Expr.BodyType.Block && bodyKind == LambdaExpressionTree.BodyKind.EXPRESSION))
            {
                throw new RuntimeException(String.format("期望的lambda类型为:%s,实际为:%s\n%s", expr.value() == Expr.BodyType.Expr ? "表达式" : "代码块", expr.value() == Expr.BodyType.Expr ? "代码块" : "表达式", tree));
            }
            //
            JCTree.JCExpression built = doMakeExpression(jcLambda);
        }
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

    private JCTree.JCExpression doMakeExpression(JCTree tree)
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
            JCTree.JCExpression left = doMakeExpression(jcBinary.getLeftOperand());
            JCTree.JCExpression right = doMakeExpression(jcBinary.getRightOperand());
            return treeMaker.App(
                    getFactoryMethod(Binary, Arrays.asList(Expression.class, Expression.class, OperatorType.class)),
                    List.of(left, right, getOperator(jcBinary.getTag()))
            );
        }
        else if (tree instanceof JCTree.JCMethodInvocation)
        {
            JCTree.JCMethodInvocation jcMethodInvocation = (JCTree.JCMethodInvocation) tree;
            JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
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
                    args.append(doMakeExpression(argument));
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
                                    List.of(treeMaker.This(thiz.type), treeMaker.Literal("this"))
                            )
                    );
                }
            }
            else
            {
                JCTree.JCFieldAccess select = (JCTree.JCFieldAccess) methodSelect;
                symbol = select.sym.location();
                of.append(doMakeExpression(select.getExpression()));
            }

            Type.MethodType methodType = methodSelect.type.asMethodType();
            String methodName = methodSelect instanceof JCTree.JCFieldAccess
                    ? ((JCTree.JCFieldAccess) methodSelect).getIdentifier().toString()
                    : methodSelect.toString();

            ListBuffer<JCTree.JCExpression> ts = new ListBuffer<>();
            for (Type parameterType : methodSelect.type.asMethodType().getParameterTypes())
            {
                ts.add(treeMaker.ClassLiteral(parameterType));
            }

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
            //System.out.println(jcFieldAccess.sym.getKind());
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
                                doMakeExpression(jcFieldAccess.getExpression()),
                                reflectField(jcFieldAccess.getExpression().type, jcFieldAccess.getIdentifier().toString())
                        )
                );
            }
        }
        else if (tree instanceof JCTree.JCParens)
        {
            JCTree.JCParens jcParens = (JCTree.JCParens) tree;
            JCTree.JCExpression expr = doMakeExpression(jcParens.getExpression());
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
                args.append(doMakeExpression(statement));
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
            return doMakeExpression(jcExpressionStatement.getExpression());
        }
        else if (tree instanceof JCTree.JCUnary)
        {
            JCTree.JCUnary jcUnary = (JCTree.JCUnary) tree;
            JCTree.JCExpression expr = doMakeExpression(jcUnary.getExpression());
            return treeMaker.App(
                    getFactoryMethod(Unary, Arrays.asList(Expression.class, OperatorType.class)),
                    List.of(expr, getOperator(jcUnary.getTag()))
            );
        }
        else if (tree instanceof JCTree.JCAssign)
        {
            JCTree.JCAssign jcAssign = (JCTree.JCAssign) tree;
            JCTree.JCExpression left = doMakeExpression(jcAssign.getVariable());
            JCTree.JCExpression right = doMakeExpression(jcAssign.getExpression());
            return treeMaker.App(
                    getFactoryMethod(Assign, Arrays.asList(Expression.class, Expression.class)),
                    List.of(left, right)
            );
        }
        else if (tree instanceof JCTree.JCAssignOp)
        {
            JCTree.JCAssignOp jcAssignOp = (JCTree.JCAssignOp) tree;
            JCTree.JCExpression left = doMakeExpression(jcAssignOp.getVariable());
            JCTree.JCExpression right = doMakeExpression(jcAssignOp.getExpression());
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
                args.append(doMakeExpression(jcVariableDecl.getInitializer()));
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
            JCTree.JCExpression indexed = doMakeExpression(jcArrayAccess.getExpression());
            JCTree.JCExpression index = doMakeExpression(jcArrayAccess.getIndex());
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
                args.append(doMakeExpression(argument));
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
                    JCTree.JCExpression variable = doMakeExpression(variableDecl);
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
                dims.append(doMakeExpression(dimension));
            }
            for (JCTree.JCExpression initializer : jcNewArray.getInitializers())
            {
                args.append(doMakeExpression(initializer));
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
            JCTree.JCExpression result = doMakeExpression(jcReturn.getExpression());
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
            JCTree.JCExpression cond = doMakeExpression(jcConditional.getCondition());
            JCTree.JCExpression ifTrue = doMakeExpression(jcConditional.getTrueExpression());
            JCTree.JCExpression ifFalse = doMakeExpression(jcConditional.getFalseExpression());
            return treeMaker.App(
                    getFactoryMethod(Conditional, Arrays.asList(Expression.class, Expression.class, Expression.class)),
                    List.of(cond, ifTrue, ifFalse)
            );
        }
        else if (tree instanceof JCTree.JCIf)
        {
            JCTree.JCIf jcIf = (JCTree.JCIf) tree;
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            JCTree.JCExpression cond = doMakeExpression(jcIf.getCondition());
            args.append(cond);
            if (jcIf.getThenStatement() != null)
            {
                JCTree.JCExpression then = doMakeExpression(jcIf.getThenStatement());
                args.append(then);
            }
            else
            {
                args.append(getNull());
            }
            if (jcIf.getElseStatement() != null)
            {
                JCTree.JCExpression elSe = doMakeExpression(jcIf.getElseStatement());
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
                inits.append(doMakeExpression(jcStatement));
            }
            args.append(makeArray(Expression.class, inits.toList()));
            if (jcForLoop.getCondition() != null)
            {
                args.append(doMakeExpression(jcForLoop.getCondition()));
            }
            else
            {
                args.append(getNull());
            }
            ListBuffer<JCTree.JCExpression> steps = new ListBuffer<>();
            for (JCTree.JCExpressionStatement expressionStatement : jcForLoop.getUpdate())
            {
                steps.append(doMakeExpression(expressionStatement));
            }
            args.append(makeArray(Expression.class, steps.toList()));
            if (jcForLoop.getStatement() != null)
            {
                args.append(doMakeExpression(jcForLoop.getStatement()));
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
            JCTree.JCExpression var = doMakeExpression(jcEnhancedForLoop.getVariable());
            JCTree.JCExpression expr = doMakeExpression(jcEnhancedForLoop.getExpression());
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            args.append(var).append(expr);
            if (jcEnhancedForLoop.getStatement() != null)
            {
                args.append(doMakeExpression(jcEnhancedForLoop.getStatement()));
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
            args.append(doMakeExpression(jcWhileLoop.getCondition()));
            if (jcWhileLoop.getStatement() != null)
            {
                args.append(doMakeExpression(jcWhileLoop.getStatement()));
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
            JCTree.JCExpression selector = doMakeExpression(jcSwitch.getExpression());
            ListBuffer<JCTree.JCExpression> cases = new ListBuffer<>();
            for (JCTree.JCCase aCase : jcSwitch.getCases())
            {
                cases.append(doMakeExpression(aCase));
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
            JCTree.JCExpression part = doMakeExpression(jcCase.getExpression());
            ListBuffer<JCTree.JCExpression> stats = new ListBuffer<>();
            for (JCTree.JCStatement statement : jcCase.getStatements())
            {
                stats.append(doMakeExpression(statement));
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
            args.append(doMakeExpression(jcTry.getBlock()));
            ListBuffer<JCTree.JCExpression> catches = new ListBuffer<>();
            for (JCTree.JCCatch aCatch : jcTry.getCatches())
            {
                catches.append(doMakeExpression(aCatch));
            }
            args.append(makeArray(CatchExpression.class, catches.toList()));
            if (jcTry.getFinallyBlock() != null)
            {
                args.append(doMakeExpression(jcTry.getFinallyBlock()));
            }
            else
            {
                args.append(getNull());
            }
            ListBuffer<JCTree.JCExpression> resources = new ListBuffer<>();
            for (JCTree resource : jcTry.getResources())
            {
                resources.append(doMakeExpression(resource));
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
            JCTree.JCExpression param = doMakeExpression(jcCatch.getParameter());
            JCTree.JCExpression body = doMakeExpression(jcCatch.getBlock());
            return treeMaker.App(
                    getFactoryMethod(Catch, Arrays.asList(VariableExpression.class, BlockExpression.class)),
                    List.of(param, body)
            );
        }
        else if (tree instanceof JCTree.JCThrow)
        {
            JCTree.JCThrow jcThrow = (JCTree.JCThrow) tree;
            JCTree.JCExpression expr = doMakeExpression(jcThrow.getExpression());
            return treeMaker.App(
                    getFactoryMethod(Throw, Collections.singletonList(Expression.class)),
                    List.of(expr)
            );
        }
        else if (tree instanceof JCTree.JCTypeCast)
        {
            JCTree.JCTypeCast jcTypeCast = (JCTree.JCTypeCast) tree;
            JCTree.JCExpression target = doMakeExpression(jcTypeCast.getType());
            JCTree.JCExpression expr = doMakeExpression(jcTypeCast.getExpression());
            return treeMaker.App(
                    getFactoryMethod(TypeCast, Arrays.asList(Class.class, Expression.class)),
                    List.of(target, expr)
            );
        }
        throw new RuntimeException("不支持的类型:" + tree.type + "\n" + tree);
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

    private Symbol.ClassSymbol getClassSymbol(Class<?> clazz)
    {
        Name name = names.fromString(clazz.getName());
        Symbol.ClassSymbol classSymbol;
        if (JDK.is17orLater())
        {
            //classSymbol = ReflectUtil.invokeMethod(classReader, "enterClass", Collections.singletonList(name));
            classSymbol = ReflectUtil.invokeMethod(symtab, "enterClass", Arrays.asList(moduleSymbol, name));
        }
        else if (JDK.is9orLater())
        {
//            classSymbol = ReflectUtil.invokeMethod(symtab, "getClass", Arrays.asList(moduleSymbol, name));
//            if (classSymbol == null)
//            {
//                //classSymbol = ReflectUtil.invokeMethod(javaCompiler, "resolveIdent", Arrays.asList(moduleSymbol, clazz.getName()));
//                //classSymbol = classReader.enterClass(name);
//                classSymbol = ReflectUtil.invokeMethod(classReader, "enterClass", Collections.singletonList(name));
//            }
            classSymbol = ReflectUtil.invokeMethod(symtab, "enterClass", Arrays.asList(moduleSymbol, name));
        }
        else
        {
            classSymbol = ReflectUtil.<Map<Name, Symbol.ClassSymbol>>getFieldValue(symtab, "classes").get(name);
            if (classSymbol == null)
            {
                classSymbol = classReader.enterClass(name);
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

    private JCTree.JCFieldAccess refMakeSelector(JCTree.JCExpression base, Symbol sym)
    {
        return ReflectUtil.invokeMethod(treeMaker, "Select", Arrays.asList(base, sym));
    }
}
