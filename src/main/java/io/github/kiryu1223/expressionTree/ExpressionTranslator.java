package io.github.kiryu1223.expressionTree;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressionV2.IExpression;
import io.github.kiryu1223.expressionTree.expressionV2.NewExpression;
import io.github.kiryu1223.expressionTree.info.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class ExpressionTranslator extends TreeTranslator
{
    private final java.util.List<VarInfo> currentVarInfos;
    private final java.util.List<ClassInfo> classInfos;
    private final Map<JCTree.Tag, JCTree.JCFieldAccess> opMap;
    private final Map<IExpression.Type, JCTree.JCFieldAccess> expressionMap;
    private final TreeMaker treeMaker;
    private final Names names;
    private final ClassInfo currentClassInfo;
    private final ThreadLocal<JCTree.JCFieldAccess> localClass = new ThreadLocal<>();
    private final java.util.List<String> LambdaParam = new ArrayList<>();
    private final java.util.List<String> LambdaLocalVar = new ArrayList<>();
    private final java.util.List<VarInfo> methodLocalVar = new ArrayList<>();

    public ExpressionTranslator(
            java.util.List<VarInfo> currentVarInfos, java.util.List<ClassInfo> classInfos, Map<JCTree.Tag, JCTree.JCFieldAccess> opMap,
            Map<IExpression.Type, JCTree.JCFieldAccess> expressionMap, TreeMaker treeMaker, Names names, ClassInfo currentClassInfo)
    {
        this.currentVarInfos = currentVarInfos;
        this.classInfos = classInfos;
        this.opMap = opMap;
        this.expressionMap = expressionMap;
        this.treeMaker = treeMaker;
        this.names = names;
        this.currentClassInfo = currentClassInfo;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree)
    {
        super.visitMethodDef(tree);

        for (JCTree.JCVariableDecl parameter : tree.getParameters())
        {
            methodLocalVar.add(new VarInfo(
                    parameter.getPreferredPosition(),
                    parameter.getName().toString(),
                    findFullName(parameter.getType().toString())
            ));
        }

        for (JCTree.JCStatement statement : tree.getBody().getStatements())
        {
            statement.accept(new TreeTranslator()
            {
                @Override
                public void visitApply(JCTree.JCMethodInvocation invocation)
                {
                    List<JCTree.JCExpression> arguments = invocation.getArguments();
                    if (hasLambda(arguments))
                    {
                        if (invocation.getMethodSelect() instanceof JCTree.JCFieldAccess)
                        {
                            JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess) invocation.getMethodSelect();
                            Collections.reverse(methodLocalVar);
                            String typeName = findType(methodLocalVar, methodSelect.getExpression());
                            if (typeName != null)
                            {
                                String methodName = methodSelect.getIdentifier().toString();
                                for (ClassInfo classInfo : classInfos)
                                {
                                    if (classInfo.getFullName().equals(typeName))
                                    {
                                        findMethodAndChangeIt(invocation, arguments, methodName, classInfo);
                                    }
                                }
                            }
                            Collections.reverse(methodLocalVar);
                        }
                        else if (invocation.getMethodSelect() instanceof JCTree.JCIdent)
                        {
                            JCTree.JCIdent methodSelect = (JCTree.JCIdent) invocation.getMethodSelect();
                            String methodName = methodSelect.getName().toString();
                            findMethodAndChangeIt(invocation, arguments, methodName, currentClassInfo);
                        }
                    }
                    super.visitApply(invocation);
                }
            });
            if (statement instanceof JCTree.JCVariableDecl)
            {
                JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) statement;
                int pos = variableDecl.getPreferredPosition();
                String name = variableDecl.getName().toString();
                if (variableDecl.getType() == null)
                {
                    JCTree.JCExpression initializer = variableDecl.getInitializer();
                    if (initializer != null)
                    {
                        Collections.reverse(methodLocalVar);
                        String typeName = findType(methodLocalVar, initializer);
                        Collections.reverse(methodLocalVar);
                        if (typeName != null)
                        {
                            methodLocalVar.add(new VarInfo(pos, name, typeName));
                        }
                    }
                }
                else
                {
                    String typeFullName = findFullName(variableDecl.getType().toString());
                    methodLocalVar.add(new VarInfo(pos, name, typeFullName));
                }
            }
        }

        methodLocalVar.clear();
    }

    private boolean hasLambda(List<JCTree.JCExpression> arguments)
    {
        for (JCTree.JCExpression argument : arguments)
        {
            if (argument instanceof JCTree.JCLambda)
            {
                return true;
            }
        }
        return false;
    }

    private void findMethodAndChangeIt(JCTree.JCMethodInvocation invocation, List<JCTree.JCExpression> arguments, String methodName, ClassInfo currentClassInfo)
    {
        for (MethodInfo methodInfo : currentClassInfo.getMethodInfos())
        {
            java.util.List<ParamInfo> paramInfos = methodInfo.getParamInfos();
            if (methodInfo.getMethodName().equals(methodName)
                    && paramInfos.size() == arguments.size())
            {
                for (int i = 0; i < paramInfos.size(); i++)
                {
                    JCTree.JCExpression expression = arguments.get(i);
                    if (!(expression instanceof JCTree.JCLambda)) continue;
                    JCTree.JCLambda jcLambda = (JCTree.JCLambda) expression;
                    ParamInfo paramInfo = paramInfos.get(i);
                    if (!paramInfo.isExpressionParam()) continue;
                    for (ClassInfo classInfo : classInfos)
                    {
                        if (classInfo.getFullName().equals(paramInfo.getParamType())
                                && classInfo.getMethodInfos().size() == 1)
                        {
                            int paramCount = classInfo.getMethodInfos().get(0).getParamInfos().size();
                            if (jcLambda.getParameters().size() == paramCount)
                            {
                                extracted(invocation, i, jcLambda, paramInfo.getExpressionType());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private String findType(java.util.List<VarInfo> varInfos, JCTree.JCExpression expression)
    {
        if (expression instanceof JCTree.JCNewClass)
        {
            JCTree.JCNewClass jcNewClass = (JCTree.JCNewClass) expression;
            return findFullName(jcNewClass.getIdentifier().toString());
        }
        else if (expression instanceof JCTree.JCIdent)
        {
            JCTree.JCIdent jcIdent = (JCTree.JCIdent) expression;
            String varName = jcIdent.toString();
            for (VarInfo varInfo : varInfos)
            {
                if (varInfo.varName.equals(varName) && varInfo.pos < expression.getPreferredPosition())
                {
                    return varInfo.typeName;
                }
            }
            for (VarInfo currentVarInfo : currentVarInfos)
            {
                if (currentVarInfo.varName.equals(varName))
                {
                    return currentVarInfo.typeName;
                }
            }
            return findFullName(varName);
        }
        else if (expression instanceof JCTree.JCMethodInvocation)
        {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) expression;
            if (methodInvocation.getMethodSelect() instanceof JCTree.JCFieldAccess)
            {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodInvocation.getMethodSelect();
                String methodName = fieldAccess.getIdentifier().toString();
                String typeName = findType(varInfos, fieldAccess.getExpression());
                if (typeName == null) return null;
                for (ClassInfo classInfo : classInfos)
                {
                    if (classInfo.getFullName().equals(typeName))
                    {
                        for (MethodInfo methodInfo : classInfo.getMethodInfos())
                        {
                            if (methodInfo.getMethodName().equals(methodName)
                                    && methodInfo.getParamInfos().size()
                                    == methodInvocation.getArguments().size())
                            {
                                return methodInfo.getReturnType();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void extracted(JCTree.JCMethodInvocation invocation, int index, JCTree.JCLambda lambda, Class<? extends IExpression> ie)
    {
        JCTree.JCExpression body;
        if (ie == NewExpression.class)
        {
            body = tryGetNewExpression(lambda);
        }
        else
        {
            body = tryGetExpression(lambda);
        }
        ListBuffer<JCTree.JCVariableDecl> decls = new ListBuffer<JCTree.JCVariableDecl>();
        JCTree.JCVariableDecl def = treeMaker.VarDef(
                treeMaker.Modifiers(0),
                names.fromString("unused"), null, null
        );
        decls.add(def);
        decls.addAll(lambda.params);
        JCTree.JCLambda lam = treeMaker.Lambda(decls.toList(), body);
        lam.setPos(lambda.getPreferredPosition());
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        for (int i = 0; i < invocation.args.size(); i++)
        {
            if (i == index)
            {
                args.add(lam);
                continue;
            }
            args.add(invocation.args.get(i));
        }
        invocation.args = args.toList();
    }

    private JCTree.JCMethodInvocation tryGetNewExpression(JCTree.JCLambda lambda)
    {
        JCTree.JCMethodInvocation r = tryGetExpression(lambda);
        if (!r.getMethodSelect().equals(expressionMap.get(IExpression.Type.New)))
        {
            if (r.getMethodSelect().equals(expressionMap.get(IExpression.Type.Reference)))
            {
                r = treeMaker.Apply(
                        List.nil(),
                        expressionMap.get(IExpression.Type.New),
                        r.getArguments()
                );
            }
            else
            {
                throw new RuntimeException(lambda.toString());
            }
        }
        return r;
    }

    private JCTree.JCMethodInvocation tryGetExpression(JCTree.JCLambda lambda)
    {
        LambdaLocalVar.clear();
        LambdaParam.clear();
        for (VariableTree parameter : lambda.getParameters())
        {
            JCTree.JCVariableDecl variableDecl= (JCTree.JCVariableDecl) parameter;
            LambdaParam.add(variableDecl.getName().toString());
        }
        JCTree.JCExpression r = doStart(lambda.getBody());
        if (r instanceof JCTree.JCMethodInvocation)
        {
            return (JCTree.JCMethodInvocation) r;
        }
        else
        {
            throw new RuntimeException(lambda + " " + r);
        }
    }

    private JCTree.JCExpression doStart(JCTree tree)
    {
        if (tree instanceof JCTree.JCBinary)
        {
            JCTree.JCBinary binary = (JCTree.JCBinary) tree;
            JCTree.JCExpression left = doStart(binary.getLeftOperand());
            JCTree.JCExpression right = doStart(binary.getRightOperand());
            JCTree.JCFieldAccess op = opMap.get(binary.getTag());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Binary),
                    List.of(left, right, op)
            );
        }
        else if (tree instanceof JCTree.JCUnary)
        {
            JCTree.JCUnary unary = (JCTree.JCUnary) tree;
            JCTree.JCExpression value = doStart(unary.getExpression());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Unary),
                    List.of(value, opMap.get(unary.getTag()))
            );
        }
        else if (tree instanceof JCTree.JCIdent)
        {
            JCTree.JCIdent ident = (JCTree.JCIdent) tree;
            String id = ident.getName().toString();
            //先先先看一下是不是lambda表达式的入参
            if (LambdaParam.contains(id))
            {
                return treeMaker.Apply(
                        List.nil(),
                        expressionMap.get(IExpression.Type.Reference),
                        List.of(ident)
                );
            }
            //先看一下是不是lambda内部声明的变量
            if (LambdaLocalVar.contains(id))
            {
                return treeMaker.Apply(
                        List.nil(),
                        expressionMap.get(IExpression.Type.LocalReference),
                        List.of(treeMaker.Literal(id))
                );
            }
            //再看是不是方法内变量
            for (VarInfo varInfo : methodLocalVar)
            {
                if (varInfo.varName.equals(id))
                {
                    return treeMaker.Apply(
                            List.nil(),
                            expressionMap.get(IExpression.Type.Reference),
                            List.of(ident)
                    );
                }
            }
            //再看是不是类级变量
            for (VarInfo varInfo : currentVarInfos)
            {
                if (varInfo.varName.equals(id))
                {
                    return treeMaker.Apply(
                            List.nil(),
                            expressionMap.get(IExpression.Type.Reference),
                            List.of(ident)
                    );
                }
            }
            //再看看远处的理塘吧家人们
            String fullName = currentClassInfo.getPackageName() + "." + id;
            for (ClassInfo classInfo : classInfos)
            {
                if (classInfo.getFullName().equals(fullName))
                {
                    return treeMaker.Apply(
                            List.nil(),
                            expressionMap.get(IExpression.Type.Reference),
                            List.of(treeMaker.Select(ident, names._class))
                    );
                }
            }
        }
        else if (tree instanceof JCTree.JCLiteral)
        {
            JCTree.JCLiteral literal = (JCTree.JCLiteral) tree;
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Value),
                    List.of(literal)
            );
        }
        else if (tree instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) tree;
            JCTree.JCExpression selector = doStart(fieldAccess.getExpression());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.FieldSelect),
                    List.of(selector, treeMaker.Literal(fieldAccess.getIdentifier().toString()))
            );
        }
        else if (tree instanceof JCTree.JCMethodInvocation)
        {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) tree;
            ListBuffer<JCTree.JCExpression> listBuffer = new ListBuffer<>();

            if (methodInvocation.getMethodSelect() instanceof JCTree.JCFieldAccess)
            {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodInvocation.getMethodSelect();
                JCTree.JCExpression selector = doStart(fieldAccess.getExpression());
                listBuffer.add(selector);
                listBuffer.add(treeMaker.Literal(fieldAccess.getIdentifier().toString()));
            }
            else if (methodInvocation.getMethodSelect() instanceof JCTree.JCIdent)
            {
                JCTree.JCIdent ident = (JCTree.JCIdent) methodInvocation.getMethodSelect();
                JCTree.JCFieldAccess loc = localClass.get();
                JCTree.JCMethodInvocation thiz = treeMaker.Apply(
                        List.nil(),
                        expressionMap.get(IExpression.Type.Reference),
                        List.of(loc == null ? treeMaker.Ident(names._this) : loc)
                );
                listBuffer.add(thiz);
                listBuffer.add(treeMaker.Literal(ident.getName().toString()));
            }

            for (JCTree.JCExpression argument : methodInvocation.getArguments())
            {
                listBuffer.add(doStart(argument));
            }

            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.MethodCall),
                    listBuffer.toList()
            );
        }
        else if (tree instanceof JCTree.JCNewClass)
        {
            JCTree.JCNewClass newClass = (JCTree.JCNewClass) tree;
            ListBuffer<JCTree.JCExpression> listBuffer = new ListBuffer<>();
            JCTree.JCFieldAccess loc = treeMaker.Select(newClass.getIdentifier(), names.fromString("class"));
            listBuffer.append(loc);
            if (newClass.getClassBody() != null && !newClass.getClassBody().getMembers().isEmpty())
            {
                localClass.set(loc);
                JCTree.JCBlock member = (JCTree.JCBlock) newClass.getClassBody().getMembers().get(0);
                for (JCTree.JCStatement statement : member.getStatements())
                {
                    JCTree.JCExpressionStatement expressionStatement = (JCTree.JCExpressionStatement) statement;
                    listBuffer.add(doStart(expressionStatement.getExpression()));
                }
                localClass.remove();
            }
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.New),
                    listBuffer.toList()
            );
        }
        else if (tree instanceof JCTree.JCTypeCast)
        {
            JCTree.JCTypeCast typeCast = (JCTree.JCTypeCast) tree;
            return doStart(typeCast.getExpression());
        }
        else if (tree instanceof JCTree.JCParens)
        {
            JCTree.JCParens parens = (JCTree.JCParens) tree;
            JCTree.JCExpression val = doStart(parens.getExpression());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Parens),
                    List.of(val)
            );
        }
        else if (tree instanceof JCTree.JCAssign)
        {
            JCTree.JCAssign jcAssign = (JCTree.JCAssign) tree;
            JCTree.JCExpression left = doStart(jcAssign.getVariable());
            JCTree.JCExpression right = doStart(jcAssign.getExpression());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Assign),
                    List.of(left, right)
            );
        }
        else if (tree instanceof JCTree.JCExpressionStatement)
        {
            JCTree.JCExpressionStatement jcExpressionStatement = (JCTree.JCExpressionStatement) tree;
            return doStart(jcExpressionStatement.getExpression());
        }
        else if (tree instanceof JCTree.JCBlock)
        {
            JCTree.JCBlock block = (JCTree.JCBlock) tree;
            ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
            for (JCTree.JCStatement statement : block.getStatements())
            {
                JCTree.JCExpression r = doStart(statement);
                if (r instanceof JCTree.JCMethodInvocation)
                {
                    args.add(r);
                }
                else
                {
                    throw new RuntimeException(statement + "\n\n" + r);
                }
            }
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Block),
                    args.toList()
            );
        }
        else if (tree instanceof JCTree.JCVariableDecl)
        {
            JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) tree;
            String name = variableDecl.getName().toString();
            JCTree.JCExpression init = doStart(variableDecl.getInitializer());
            LambdaLocalVar.add(name);
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Var),
                    List.of(treeMaker.Literal(name), init)
            );
        }
        else if (tree instanceof JCTree.JCArrayAccess)
        {
            JCTree.JCArrayAccess arrayAccess = (JCTree.JCArrayAccess) tree;
            JCTree.JCExpression indexed = doStart(arrayAccess.getExpression());
            JCTree.JCExpression index = doStart(arrayAccess.getIndex());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.ArrayAccess),
                    List.of(indexed, index)
            );
        }
        else if (tree instanceof JCTree.JCIf)
        {
            JCTree.JCIf If = (JCTree.JCIf) tree;
            JCTree.JCExpression cond = doStart(If.getCondition());
            JCTree.JCExpression body = doStart(If.getThenStatement());
            JCTree.JCExpression elSe = doStart(If.getElseStatement());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.If),
                    List.of(
                            cond,
                            body != null ? body : treeMaker.Literal(TypeTag.BOT),
                            elSe != null ? elSe : treeMaker.Literal(TypeTag.BOT)
                    )
            );
        }
        else if (tree instanceof JCTree.JCReturn)
        {
            JCTree.JCReturn jcReturn = (JCTree.JCReturn) tree;
            JCTree.JCExpression expression = doStart(jcReturn.getExpression());
            return treeMaker.Apply(
                    List.nil(),
                    expressionMap.get(IExpression.Type.Return),
                    List.of(expression)
            );
        }
        else
        {
            System.out.println(tree);
            System.out.println(tree.getClass());
            System.out.println("-----------------------");
        }
        return null;
    }

    private String findFullName(String typeName)
    {
        String[] typeSp = typeName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (ImportInfo Import : currentClassInfo.getImportInfos())
        {
            String importName = Import.getImportName();
            String[] sp = importName.split("\\.");
            if (sp[sp.length - 1].equals(typeSp[0]))
            {
                sb.append(importName);
                for (int i = 1; i < typeSp.length; i++)
                {
                    sb.append(".").append(typeSp[i]);
                }
                break;
            }
        }
        if (sb.length() != 0)
        {
            return sb.toString();
        }
        String fullName = currentClassInfo.getPackageName() + "." + typeName;
        for (ClassInfo classInfo : classInfos)
        {
            if (classInfo.getFullName().equals(fullName))
            {
                return classInfo.getFullName();
            }
        }
        return typeName;
    }

    private ClassInfo getClassInfo()
    {
        return null;
    }
}
