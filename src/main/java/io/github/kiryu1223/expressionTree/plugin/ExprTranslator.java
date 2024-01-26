package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.*;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ExprTranslator extends TreeScanner
{
    private final java.util.List<ImportInfo> imports;
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Map<String, JCTree.JCVariableDecl> classFields;
    private final Map<JCTree.JCFieldAccess, JCTree> needToChangeClasses;
    private final java.util.List<JCTree.JCMethodInvocation> needToChangeRef = new ArrayList<>();
    private final Map<Kind, JCTree.JCFieldAccess> methods;
    private final Map<JCTree.Tag, JCTree.JCFieldAccess> operators;
    private int index = 0;
    private final String exprName = ExprTree.class.getCanonicalName();
    private final String exprStar = ExprTree.class.getPackage().getName() + ".*";
    private final JCTree.JCFieldAccess parameter;
    private final JCTree.JCFieldAccess reflectUtil;
    private final Stack<JCTree.JCClassDecl> classDeclStack = new Stack<>();

    public ExprTranslator(java.util.List<ImportInfo> imports, TreeMaker treeMaker, Types types, Names names, Map<String, JCTree.JCVariableDecl> classFields, Map<JCTree.JCFieldAccess, JCTree> needToChangeClasses, Map<Kind, JCTree.JCFieldAccess> methods, Map<JCTree.Tag, JCTree.JCFieldAccess> operators)
    {
        this.imports = imports;
        this.treeMaker = treeMaker;
        this.types = types;
        this.names = names;
        this.classFields = classFields;
        this.needToChangeClasses = needToChangeClasses;
        this.methods = methods;
        this.operators = operators;
        String parameterPath = ParameterExpression.class.getPackage().getName();
        String parameterName = ParameterExpression.class.getSimpleName();
        parameter = treeMaker.Select(
                treeMaker.Ident(names.fromString(parameterPath)),
                names.fromString(parameterName)
        );
        String reflectUtilPath = ReflectUtil.class.getPackage().getName();
        String reflectUtilName = ReflectUtil.class.getSimpleName();
        reflectUtil = treeMaker.Select(
                treeMaker.Ident(names.fromString(reflectUtilPath)),
                names.fromString(reflectUtilName)
        );
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl methodDecl)
    {
        index = 0;
        treeMaker.at(methodDecl.getStartPosition());
        Map<String, JCTree.JCVariableDecl> methodParameter = new HashMap<>();
        for (JCTree.JCVariableDecl methodDeclParameter : methodDecl.getParameters())
        {
            methodParameter.put(methodDeclParameter.getName().toString(), methodDeclParameter);
        }
        ListBuffer<JCTree.JCStatement> listBuffer = new ListBuffer<>();
        JCTree.JCBlock body = methodDecl.getBody();
        if (body != null)
        {
            for (JCTree.JCStatement statement : body.getStatements())
            {
                if (statement instanceof JCTree.JCVariableDecl)
                {
                    JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) statement;
                    methodParameter.put(jcVariableDecl.getName().toString(), jcVariableDecl);
                }
                statement.accept(new NewExprTranslator(listBuffer, methodParameter));
                listBuffer.append(statement);
            }
            methodDecl.body.stats = listBuffer.toList();
        }
        super.visitMethodDef(methodDecl);
    }

    public java.util.List<JCTree.JCMethodInvocation> getNeedToChangeRef()
    {
        return needToChangeRef;
    }

    private class NewExprTranslator extends TreeScanner
    {
        private final ListBuffer<JCTree.JCStatement> statementList;
        private final Map<String, JCTree.JCVariableDecl> methodParameter;
        private final Map<String, Name> parmap = new HashMap<>();
        private final Map<String, Name> blockVarmap = new HashMap<>();
        private final Stack<Map<String, Name>> blockVarStack = new Stack<>();

        public NewExprTranslator(ListBuffer<JCTree.JCStatement> statementList, Map<String, JCTree.JCVariableDecl> methodParameter)
        {
            this.statementList = statementList;
            this.methodParameter = methodParameter;
        }

        private boolean isExprTree(JCTree tree)
        {
            if (tree instanceof JCTree.JCNewClass)
            {
                JCTree.JCNewClass newClass = (JCTree.JCNewClass) tree;
                if (!(newClass.getIdentifier() instanceof JCTree.JCTypeApply)) return false;
                JCTree.JCTypeApply typeApply = (JCTree.JCTypeApply) newClass.getIdentifier();
                if (!typeApply.getType().toString().equals(ExprTree.class.getSimpleName())) return false;
                List<JCTree.JCExpression> args = newClass.getArguments();
                if (args.size() != 1 || args.get(0).getKind() != Tree.Kind.LAMBDA_EXPRESSION) return false;
                for (ImportInfo anImport : imports)
                {
                    if (anImport.getName().equals(exprName)
                            || anImport.getName().equals(exprStar)) return true;
                }
            }
            else if (tree instanceof JCTree.JCMethodInvocation)
            {
                JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) tree;
                List<JCTree.JCExpression> args = methodInvocation.getArguments();
                if (args.size() != 1 || args.get(0).getKind() != Tree.Kind.LAMBDA_EXPRESSION) return false;
                JCTree.JCExpression select = methodInvocation.getMethodSelect();
                return hasExprStaticImport(select.toString());
            }
            return false;
        }

        private String getNextLambdaParameter()
        {
            return "lambdaParameter_" + index++;
        }

        private String getNextBlockVar()
        {
            return "blockVar_" + index++;
        }

        private void transExprTree(JCTree tree)
        {
            if (tree instanceof JCTree.JCNewClass)
            {
                JCTree.JCNewClass newClass = (JCTree.JCNewClass) tree;
                ListBuffer<JCTree.JCExpression> args = lambdaToTree((JCTree.JCLambda) newClass.getArguments().get(0), newClass.getArguments());
                newClass.args = args.toList();
            }
            else if (tree instanceof JCTree.JCMethodInvocation)
            {
                JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) tree;
                ListBuffer<JCTree.JCExpression> args = lambdaToTree((JCTree.JCLambda) methodInvocation.getArguments().get(0), methodInvocation.getArguments());
                methodInvocation.args = args.toList();
            }
        }

        private ListBuffer<JCTree.JCExpression> lambdaToTree(JCTree.JCLambda lambda, List<JCTree.JCExpression> oldArgs)
        {
            for (VariableTree variableTree : lambda.getParameters())
            {
                JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) variableTree;
                JCTree.JCFieldAccess clazz = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
                needToChangeClasses.put(clazz, variableDecl);
                String lambdaParName = variableDecl.getName().toString();
                Name lambdaVarName = names.fromString(getNextLambdaParameter());
                parmap.put(lambdaParName, lambdaVarName);
                JCTree.JCVariableDecl lambdaVar = treeMaker.VarDef(
                        treeMaker.Modifiers(0),
                        lambdaVarName,
                        parameter,
                        treeMaker.Apply(
                                null,
                                methods.get(Kind.Parameter),
                                List.of(clazz, treeMaker.Literal(lambdaParName))
                        )
                );
                statementList.append(lambdaVar);
            }
            JCTree.JCExpression exprResult = deepVisit(lambda.getBody());
            JCTree.JCFieldAccess returnType = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
            needToChangeClasses.put(returnType, lambda);
            ListBuffer<JCTree.JCExpression> lambdaVars = new ListBuffer<>();
            parmap.forEach((k, v) ->
            {
                lambdaVars.add(treeMaker.Ident(v));
            });
            parmap.clear();
            JCTree.JCMethodInvocation finalTree = treeMaker.Apply(
                    null,
                    methods.get(Kind.Lambda),
                    List.of(
                            exprResult,
                            makeArray(ParameterExpression.class, lambdaVars.toList()),
                            returnType
                    )
            );

            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            args.appendList(oldArgs);
            args.append(finalTree);
            return args;
        }

        private JCTree.JCExpression deepVisit(JCTree tree)
        {
            if (tree instanceof JCTree.JCBinary)
            {
                JCTree.JCBinary jcBinary = (JCTree.JCBinary) tree;
                JCTree.JCExpression left = deepVisit(jcBinary.getLeftOperand());
                JCTree.JCExpression right = deepVisit(jcBinary.getRightOperand());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Binary),
                        List.of(left, right, operators.get(jcBinary.getTag()))
                );
            }
            else if (tree instanceof JCTree.JCIdent)
            {
                JCTree.JCIdent jcIdent = (JCTree.JCIdent) tree;
                String name = jcIdent.getName().toString();
                //检查是不是lambda代码块内部定义变量
                if (blockVarmap.containsKey(name))
                {
                    return treeMaker.Ident(blockVarmap.get(name));
                }
                //检查是不是lambda的入参变量
                else if (parmap.containsKey(name))
                {
                    return treeMaker.Ident(parmap.get(name));
                }
                //检查是不是代码块内的变量或者内部字段的变量
                else if (methodParameter.containsKey(name)
                        || classFields.containsKey(name))
                {
                    JCTree.JCMethodInvocation apply = treeMaker.Apply(
                            null,
                            methods.get(Kind.Reference),
                            List.of(
                                    treeMaker.Ident(names.fromString(name)),
                                    treeMaker.Literal(name),
                                    treeMaker.Literal(false)
                            )
                    );
                    needToChangeRef.add(apply);
                    return apply;
                }
                //检查是不是静态导入
                for (ImportInfo anImport : imports)
                {
                    if (anImport.isStatic())
                    {
                        String[] split = anImport.getName().split("\\.");
                        if (split[split.length - 1].equals(name))
                        {
                            JCTree.JCMethodInvocation apply = treeMaker.Apply(
                                    null,
                                    methods.get(Kind.Reference),
                                    List.of(
                                            treeMaker.Ident(names.fromString(name)),
                                            treeMaker.Literal(name),
                                            treeMaker.Literal(false)
                                    )
                            );
                            needToChangeRef.add(apply);
                            return apply;
                        }
                    }
                }
                //都不是的话推测为.class
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.StaticClass),
                        List.of(
                                treeMaker.Select(
                                        treeMaker.Ident(names.fromString(name)),
                                        names._class
                                )
                        )
                );
            }
            else if (tree instanceof JCTree.JCLiteral)
            {
                JCTree.JCLiteral jcLiteral = (JCTree.JCLiteral) tree;
                return treeMaker.Apply(null, methods.get(Kind.Constant), List.of(jcLiteral));
            }
            else if (tree instanceof JCTree.JCMethodInvocation)
            {
                JCTree.JCMethodInvocation jcMethodInvocation = (JCTree.JCMethodInvocation) tree;
                JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
                JCTree.JCFieldAccess clazz = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
                JCTree.JCExpression prev = null;
                String methodName;
                if (methodSelect instanceof JCTree.JCFieldAccess)
                {
                    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
                    methodName = fieldAccess.getIdentifier().toString();
                    prev = deepVisit(fieldAccess.getExpression());
                    needToChangeClasses.put(clazz, fieldAccess.getExpression());
                }
                else
                {
                    methodName = methodSelect.toString();
                    if (!classDeclStack.isEmpty())
                    {
                        JCTree.JCClassDecl classPeek = classDeclStack.peek();
                        needToChangeClasses.put(clazz, classPeek);
                    }
                }
                ListBuffer<JCTree.JCExpression> types = new ListBuffer<>();
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression argument : jcMethodInvocation.getArguments())
                {
                    args.append(deepVisit(argument));
                    JCTree.JCFieldAccess argClass = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
                    types.append(argClass);
                    needToChangeClasses.put(argClass, argument);
                }
                JCTree.JCMethodInvocation methodInit = treeMaker.Apply(
                        List.nil(),
                        treeMaker.Select(reflectUtil, names.fromString("getMethod")),
                        List.of(
                                clazz,
                                treeMaker.Literal(methodName),
                                treeMaker.NewArray(
                                        treeMaker.Ident(names.fromString("Class")),
                                        List.nil(),
                                        types.toList()
                                )
                        )
                );
                JCTree.JCIdent methodParam = makeToMethodParam(methodInit);
                return treeMaker.Apply(
                        List.nil(),
                        methods.get(Kind.MethodCall),
                        List.of(
                                prev != null ? prev : treeMaker.Literal(TypeTag.BOT, null),
                                methodParam,
                                makeArray(Expression.class, args.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCBlock)
            {
                JCTree.JCBlock jcBlock = (JCTree.JCBlock) tree;
                blockVarStack.push(new HashMap<>());
                ListBuffer<JCTree.JCExpression> states = new ListBuffer<>();
                for (JCTree.JCStatement statement : jcBlock.getStatements())
                {
                    states.append(deepVisit(statement));
                }
                ListBuffer<JCTree.JCExpression> vars = new ListBuffer<>();
                Map<String, Name> pop = blockVarStack.pop();
                pop.forEach((k, v) ->
                {
                    vars.append(treeMaker.Ident(v));
                });
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Block),
                        List.of(
                                makeArray(Expression.class, states.toList()),
                                makeArray(ParameterExpression.class, vars.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCExpressionStatement)
            {
                JCTree.JCExpressionStatement jcExpressionStatement = (JCTree.JCExpressionStatement) tree;
                return deepVisit(jcExpressionStatement.getExpression());
            }
            else if (tree instanceof JCTree.JCFieldAccess)
            {
                JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) tree;
                JCTree.JCExpression prev = deepVisit(jcFieldAccess.getExpression());
                JCTree.JCFieldAccess clazz = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
                needToChangeClasses.put(clazz, jcFieldAccess.getExpression());
                JCTree.JCMethodInvocation fieldInit = treeMaker.Apply(
                        List.nil(),
                        treeMaker.Select(reflectUtil, names.fromString("getField")),
                        List.of(
                                clazz,
                                treeMaker.Literal(jcFieldAccess.getIdentifier().toString())
                        )
                );
                JCTree.JCIdent fieldParam = makeToFieldParam(fieldInit);
                return treeMaker.Apply(
                        List.nil(),
                        methods.get(Kind.FieldSelect),
                        List.of(
                                prev != null ? prev : treeMaker.Literal(TypeTag.BOT, null),
                                fieldParam
                        )
                );
            }
            else if (tree instanceof JCTree.JCUnary)
            {
                JCTree.JCUnary jcUnary = (JCTree.JCUnary) tree;
                JCTree.JCExpression expr = deepVisit(jcUnary.getExpression());
                JCTree.JCFieldAccess fieldAccess = operators.get(jcUnary.getTag());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Unary),
                        List.of(expr, fieldAccess)
                );
            }
            else if (tree instanceof JCTree.JCAssign)
            {
                JCTree.JCAssign jcAssign = (JCTree.JCAssign) tree;
                JCTree.JCExpression left = deepVisit(jcAssign.getVariable());
                JCTree.JCExpression right = deepVisit(jcAssign.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Assign),
                        List.of(left, right)
                );
            }
            else if (tree instanceof JCTree.JCAssignOp)
            {
                JCTree.JCAssignOp jcAssignOp = (JCTree.JCAssignOp) tree;
                JCTree.JCExpression left = deepVisit(jcAssignOp.getVariable());
                JCTree.JCExpression right = deepVisit(jcAssignOp.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.AssignOp),
                        List.of(left, right, operators.get(jcAssignOp.getTag()))
                );
            }
            else if (tree instanceof JCTree.JCVariableDecl)
            {
                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                JCTree.JCFieldAccess clazz = treeMaker.Select(treeMaker.TypeIdent(TypeTag.VOID), names._class);
                needToChangeClasses.put(clazz, jcVariableDecl);
                String blockVar = jcVariableDecl.getName().toString();
                Name blockVarVarName = names.fromString(getNextBlockVar());
                blockVarmap.put(blockVar, blockVarVarName);
                blockVarStack.peek().put(blockVar, blockVarVarName);
                JCTree.JCVariableDecl blockVarVar = treeMaker.VarDef(
                        treeMaker.Modifiers(0),
                        blockVarVarName,
                        parameter,
                        treeMaker.Apply(
                                null,
                                methods.get(Kind.Parameter),
                                List.of(
                                        clazz,
                                        treeMaker.Literal(blockVar)
                                )
                        )
                );
                statementList.append(blockVarVar);
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(treeMaker.Ident(blockVarVarName));
                if (jcVariableDecl.getInitializer() != null)
                {
                    args.append(deepVisit(jcVariableDecl.getInitializer()));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Variable),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCArrayAccess)
            {
                JCTree.JCArrayAccess jcArrayAccess = (JCTree.JCArrayAccess) tree;
                JCTree.JCExpression indexed = deepVisit(jcArrayAccess.getExpression());
                JCTree.JCExpression index = deepVisit(jcArrayAccess.getIndex());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Index),
                        List.of(indexed, index)
                );
            }
            else if (tree instanceof JCTree.JCNewClass)
            {
                JCTree.JCNewClass jcNewClass = (JCTree.JCNewClass) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression argument : jcNewClass.getArguments())
                {
                    args.append(deepVisit(argument));
                }
                ListBuffer<JCTree.JCExpression> mArgs = new ListBuffer<>();
                mArgs.append(treeMaker.Select(jcNewClass.getIdentifier(), names._class))
                        .append(makeArray(Expression.class, args.toList()));
                JCTree.JCClassDecl classBody = jcNewClass.getClassBody();
                if (classBody != null)
                {
                    ListBuffer<JCTree.JCExpression> elems = new ListBuffer<>();
                    classDeclStack.push(classBody);
                    for (JCTree member : classBody.getMembers())
                    {
                        if (member.getKind() == Tree.Kind.BLOCK)
                        {
                            elems.append(deepVisit(member));
                        }
                    }
                    classDeclStack.pop();
                    JCTree.JCMethodInvocation body = treeMaker.Apply(
                            null,
                            methods.get(Kind.Block),
                            List.of(
                                    makeArray(Expression.class, elems.toList()),
                                    makeArray(ParameterExpression.class, List.nil())
                            )
                    );
                    mArgs.append(body);
                }
                else
                {
                    mArgs.append(treeMaker.Literal(TypeTag.BOT,null));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.New),
                        mArgs.toList()
                );
            }
            else if (tree instanceof JCTree.JCNewArray)
            {
                JCTree.JCNewArray jcNewArray = (JCTree.JCNewArray) tree;
                ListBuffer<JCTree.JCExpression> dims = new ListBuffer<>();
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                for (JCTree.JCExpression dimension : jcNewArray.getDimensions())
                {
                    dims.append(deepVisit(dimension));
                }
                for (JCTree.JCExpression initializer : jcNewArray.getInitializers())
                {
                    args.append(deepVisit(initializer));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.NewArray),
                        List.of(
                                treeMaker.Select(jcNewArray.getType(), names._class),
                                makeArray(Expression.class, dims.toList()),
                                makeArray(Expression.class, args.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCReturn)
            {
                JCTree.JCReturn jcReturn = (JCTree.JCReturn) tree;
                JCTree.JCExpression result = deepVisit(jcReturn.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Return),
                        List.of(result)
                );
            }
            else if (tree instanceof JCTree.JCBreak)
            {
                JCTree.JCBreak jcBreak = (JCTree.JCBreak) tree;
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Break),
                        List.nil()
                );
            }
            else if (tree instanceof JCTree.JCContinue)
            {
                JCTree.JCContinue jcContinue = (JCTree.JCContinue) tree;
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Continue),
                        List.nil()
                );
            }
            else if (tree instanceof JCTree.JCParens)
            {
                JCTree.JCParens jcParens = (JCTree.JCParens) tree;
                JCTree.JCExpression result = deepVisit(jcParens.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Parens),
                        List.of(result)
                );
            }
            else if (tree instanceof JCTree.JCConditional)
            {
                JCTree.JCConditional jcConditional = (JCTree.JCConditional) tree;
                JCTree.JCExpression cond = deepVisit(jcConditional.getCondition());
                JCTree.JCExpression ifTrue = deepVisit(jcConditional.getTrueExpression());
                JCTree.JCExpression ifFalse = deepVisit(jcConditional.getFalseExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Conditional),
                        List.of(cond, ifTrue, ifFalse)
                );
            }
            else if (tree instanceof JCTree.JCIf)
            {
                JCTree.JCIf jcIf = (JCTree.JCIf) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                JCTree.JCExpression cond = deepVisit(jcIf.getCondition());
                args.append(cond);
                if (jcIf.getThenStatement() != null)
                {
                    JCTree.JCExpression then = deepVisit(jcIf.getThenStatement());
                    args.append(then);
                }
                if (jcIf.getElseStatement() != null)
                {
                    JCTree.JCExpression elSe = deepVisit(jcIf.getElseStatement());
                    args.append(elSe);
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.If),
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
                    inits.append(deepVisit(jcStatement));
                }
                args.append(makeArray(Expression.class, inits.toList()));
                if (jcForLoop.getCondition() != null)
                {
                    args.append(deepVisit(jcForLoop.getCondition()));
                }
                else
                {
                    args.append(treeMaker.Literal(TypeTag.BOT, null));
                }
                ListBuffer<JCTree.JCExpression> steps = new ListBuffer<>();
                for (JCTree.JCExpressionStatement expressionStatement : jcForLoop.getUpdate())
                {
                    steps.append(deepVisit(expressionStatement));
                }
                args.append(makeArray(Expression.class, steps.toList()));
                if (jcForLoop.getStatement() != null)
                {
                    args.append(deepVisit(jcForLoop.getStatement()));
                }
                else
                {
                    args.append(treeMaker.Literal(TypeTag.BOT, null));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.For),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCEnhancedForLoop)
            {
                JCTree.JCEnhancedForLoop jcEnhancedForLoop = (JCTree.JCEnhancedForLoop) tree;
                JCTree.JCExpression var = deepVisit(jcEnhancedForLoop.getVariable());
                JCTree.JCExpression expr = deepVisit(jcEnhancedForLoop.getExpression());
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(var).append(expr);
                if (jcEnhancedForLoop.getStatement() != null)
                {
                    args.append(deepVisit(jcEnhancedForLoop.getStatement()));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Foreach),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCWhileLoop)
            {
                JCTree.JCWhileLoop jcWhileLoop = (JCTree.JCWhileLoop) tree;
                ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
                args.append(deepVisit(jcWhileLoop.getCondition()));
                if (jcWhileLoop.getStatement() != null)
                {
                    args.append(deepVisit(jcWhileLoop.getStatement()));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.While),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCSwitch)
            {
                JCTree.JCSwitch jcSwitch = (JCTree.JCSwitch) tree;
                JCTree.JCExpression selector = deepVisit(jcSwitch.getExpression());
                ListBuffer<JCTree.JCExpression> cases = new ListBuffer<>();
                for (JCTree.JCCase aCase : jcSwitch.getCases())
                {
                    cases.append(deepVisit(aCase));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Switch),
                        List.of(
                                selector,
                                makeArray(CaseExpression.class, cases.toList())
                        )
                );
            }
            else if (tree instanceof JCTree.JCCase)
            {
                JCTree.JCCase jcCase = (JCTree.JCCase) tree;
                JCTree.JCExpression part = deepVisit(jcCase.getExpression());
                ListBuffer<JCTree.JCExpression> stats = new ListBuffer<>();
                for (JCTree.JCStatement statement : jcCase.getStatements())
                {
                    stats.append(deepVisit(statement));
                }
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Case),
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
                args.append(deepVisit(jcTry.getBlock()));
                ListBuffer<JCTree.JCExpression> catches = new ListBuffer<>();
                for (JCTree.JCCatch aCatch : jcTry.getCatches())
                {
                    catches.append(deepVisit(aCatch));
                }
                args.append(makeArray(CatchExpression.class, catches.toList()));
                if (jcTry.getFinallyBlock() != null)
                {
                    args.append(deepVisit(jcTry.getFinallyBlock()));
                }
                ListBuffer<JCTree.JCExpression> resources = new ListBuffer<>();
                for (JCTree resource : jcTry.getResources())
                {
                    resources.append(deepVisit(resource));
                }
                args.append(makeArray(Expression.class, resources.toList()));
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Try),
                        args.toList()
                );
            }
            else if (tree instanceof JCTree.JCCatch)
            {
                JCTree.JCCatch jcCatch = (JCTree.JCCatch) tree;
                JCTree.JCExpression param = deepVisit(jcCatch.getParameter());
                JCTree.JCExpression body = deepVisit(jcCatch.getBlock());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Catch),
                        List.of(param, body)
                );
            }
            else if (tree instanceof JCTree.JCThrow)
            {
                JCTree.JCThrow jcThrow = (JCTree.JCThrow) tree;
                JCTree.JCExpression expr = deepVisit(jcThrow.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.Throw),
                        List.of(expr)
                );
            }
            else if (tree instanceof JCTree.JCTypeCast)
            {
                JCTree.JCTypeCast jcTypeCast = (JCTree.JCTypeCast) tree;
                JCTree.JCExpression target = deepVisit(jcTypeCast.getType());
                JCTree.JCExpression expr = deepVisit(jcTypeCast.getExpression());
                return treeMaker.Apply(
                        null,
                        methods.get(Kind.TypeCast),
                        List.of(target, expr)
                );
            }
            throw new RuntimeException("不支持的类型:" + tree);
        }

        private JCTree.JCExpression makeArray(Class<?> type, List<JCTree.JCExpression> args)
        {
            String path = type.getPackage().getName();
            String name = type.getSimpleName();
            JCTree.JCFieldAccess clazz = treeMaker.Select(
                    treeMaker.Ident(names.fromString(path)),
                    names.fromString(name)
            );
            return treeMaker.NewArray(
                    clazz,
                    List.nil(),
                    args
            );
        }

        @Override
        public void visitNewClass(JCTree.JCNewClass newClass)
        {
            if (isExprTree(newClass))
            {
                transExprTree(newClass);
            }
            super.visitNewClass(newClass);
        }

        @Override
        public void visitApply(JCTree.JCMethodInvocation methodInvocation)
        {
            if (isExprTree(methodInvocation))
            {
                transExprTree(methodInvocation);
            }
            super.visitApply(methodInvocation);
        }

        private JCTree.JCFieldAccess getClass(Class<?> type)
        {
            String path = type.getPackage().getName();
            String expr = type.getSimpleName();
            return treeMaker.Select(treeMaker.Ident(names.fromString(path)), names.fromString(expr));
        }

        private JCTree.JCIdent makeToMethodParam(JCTree.JCExpression init)
        {
            Name param = names.fromString("reflectMethodParam_" + index++);
            JCTree.JCVariableDecl arg = treeMaker.VarDef(treeMaker.Modifiers(0), param, getClass(Method.class), init);
            statementList.append(arg);
            return treeMaker.Ident(param);
        }

        private JCTree.JCIdent makeToFieldParam(JCTree.JCExpression init)
        {
            Name param = names.fromString("reflectFieldParam_" + index++);
            JCTree.JCVariableDecl arg = treeMaker.VarDef(treeMaker.Modifiers(0), param, getClass(Field.class), init);
            statementList.append(arg);
            return treeMaker.Ident(param);
        }

        private boolean hasExprStaticImport(String name)
        {
            switch (name)
            {
                case "ExprTree":
                    for (ImportInfo anImport : imports)
                    {
                        if (anImport.isStatic()
                                && anImport.getName().equals(ExprTree.class.getCanonicalName() + ".ExprTree"))
                        {
                            return true;
                        }
                    }
                    break;
                case "Expr":
                    for (ImportInfo anImport : imports)
                    {
                        if (anImport.isStatic()
                                && anImport.getName().equals(ExprTree.class.getCanonicalName() + ".Expr"))
                        {
                            return true;
                        }
                    }
                case "ExprTree.ExprTree":
                case "ExprTree.Expr":
                    for (ImportInfo anImport : imports)
                    {
                        if (anImport.getName().equals(exprName)
                                || anImport.getName().equals(exprStar))
                        {

                            return true;
                        }
                    }
                    break;
            }
            return false;
        }
    }
}
