package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.annos.Expr;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class LambdaFinder extends TreeTranslator
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
    private final AtomicInteger argIndex = new AtomicInteger();

    public LambdaFinder(TreeMaker treeMaker, Types types, Names names, Symtab symtab, ClassReader classReader, Object moduleSymbol)
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
        tree.meth = translate(tree.meth);
        Symbol.MethodSymbol methodSymbol = methodInvocationGetMethodSymbol(tree);
        List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        List<JCTree.JCExpression> jcExpressions = tree.getArguments();
        boolean changed = false;
        ListBuffer<Type> argsType = new ListBuffer<>();
        for (int i = 0; i < jcExpressions.size(); i++)
        {
            JCTree.JCExpression arg = jcExpressions.get(i);
            Symbol.VarSymbol varSymbol = parameters.get(i);
            varSymbolDeque.push(varSymbol);
            if (arg instanceof JCTree.JCLambda && varSymbol.getAnnotation(Expr.class) != null)
            {
                changed = true;
            }
            JCTree.JCExpression translate = translate(arg);
            argsType.add(translate.type);
            args.add(translate);
            varSymbolDeque.pop();
        }
        if (changed)
        {
            Symbol.MethodSymbol targetMethodSymbol = getTargetMethodSymbol(methodSymbol, argsType);
            trySetMethodSymbol(tree, targetMethodSymbol);
        }
        tree.args = args.toList();
        result = tree;
    }

    @Override
    public void visitLambda(JCTree.JCLambda tree)
    {
        if (ownerDeque.isEmpty() || varSymbolDeque.isEmpty())
        {
            tryOpenLambda(tree);
            super.visitLambda(tree);
        }
        else
        {
            Symbol.VarSymbol varSymbol = varSymbolDeque.peek();
            if (varSymbol.getAnnotation(Expr.class) != null)
            {
                LambdaTranslator lambdaTranslator = new LambdaTranslator(treeMaker, types, names, symtab, classReader, moduleSymbol, thizDeque, ownerDeque, varSymbolDeque, statementsDeque, argIndex);
                result = lambdaTranslator.translateToExprTree(tree);
            }
            else
            {
                tryOpenLambda(tree);
                super.visitLambda(tree);
            }
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

    private JCTree.JCFieldAccess refMakeSelector(JCTree.JCExpression base, Symbol sym)
    {
        return ReflectUtil.invokeMethod(treeMaker, "Select", Arrays.asList(base, sym));
    }
}
