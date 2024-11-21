package io.github.kiryu1223.expressionTree.plugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
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
        System.out.println(tree);
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
        for (int i = 0; i < jcExpressions.size(); i++)
        {
            JCTree.JCExpression arg = jcExpressions.get(i);
            varSymbolDeque.push(parameters.get(i));
            args.add(translate(arg));
            varSymbolDeque.pop();
        }
        tree.args = args.toList();
        result = tree;
    }

    @Override
    public void visitLambda(JCTree.JCLambda tree)
    {
        if (ownerDeque.isEmpty())
        {
            super.visitLambda(tree);
        }
        else
        {

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
}
