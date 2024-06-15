package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.annos.Getter;
import io.github.kiryu1223.expressionTree.expressions.annos.Setter;
import io.github.kiryu1223.expressionTree.util.JDK;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ExprTreeTaskListener implements TaskListener
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    //private final Context context;
    private final Symtab symtab;
    private final JavaCompiler javaCompiler;
    private final ClassReader classReader;

    public ExprTreeTaskListener(Context context)
    {
        treeMaker = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
        javaCompiler = JavaCompiler.instance(context);
        classReader= ClassReader.instance(context);
        //this.context = context;
    }

    @Override
    public void started(TaskEvent event)
    {
        //System.out.printf("%s 开始 %n", event.getKind());
    }

    @Override
    public void finished(TaskEvent event)
    {
        //System.out.printf("%s 结束 %n", event.getKind());
        try
        {
            getterOrSetter(event);
            blockTaskMake(event);
            lambdaToTree(event);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private void lambdaToTree(TaskEvent event)
    {
        if (event.getKind() != TaskEvent.Kind.ANALYZE) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        Object moduleSymbol = null;
        if (JDK.is9orLater())
        {
            moduleSymbol = ReflectUtil.getFieldValue(compilationUnit, "modle");
        }
        for (JCTree tree : compilationUnit.getTypeDecls())
        {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            Type thiz = classDecl.type;
            classDecl.accept(new SugarScanner(thiz, treeMaker, types, names, symtab, javaCompiler,classReader, moduleSymbol));
        }
    }

    private void blockTaskMake(TaskEvent event)
    {
        if (event.getKind() != TaskEvent.Kind.PARSE) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        for (JCTree tree : compilationUnit.getTypeDecls())
        {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            classDecl.accept(new TreeScanner()
            {
                @Override
                public void visitBlock(JCTree.JCBlock block)
                {
                    if (block.isStatic())
                    {
                        treeMaker.at(block.pos);
                        JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(
                                treeMaker.Modifiers(0),
                                names.fromString("taskMake"),
                                treeMaker.TypeIdent(TypeTag.INT),
                                null
                        );
                        ListBuffer<JCTree.JCStatement> expressions = new ListBuffer<>();
                        expressions.add(variableDecl);
                        expressions.addAll(block.getStatements());
                        block.stats = expressions.toList();
                    }
                }
            });
        }
    }

    private void getterOrSetter(TaskEvent event)
    {
        if (event.getKind() != TaskEvent.Kind.ENTER) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        com.sun.tools.javac.util.List<JCTree.JCImport> imports = compilationUnit.getImports();
        for (JCTree tree : compilationUnit.getTypeDecls())
        {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            classDecl.accept(new TreeScanner()
            {
                @Override
                public void visitNewClass(JCTree.JCNewClass newClass)
                {
                    if (newClass.getClassBody() != null)
                    {
                        JCTree.JCImport anImport = getImport(newClass.clazz.toString(), imports);
                        if (anImport == null) return;
                        // 高版本更改了qualid字段的类型，傻逼java
                        // Type type = anImport.qualid.type;
                        Type type = ReflectUtil.<JCTree>getFieldValue(anImport, "qualid").type;
                        if (type == null) return;
                        Symbol.TypeSymbol tsym = type.tsym;
                        if (tsym == null) return;
                        JCTree.JCClassDecl classBody = newClass.getClassBody();
                        ListBuffer<JCTree.JCVariableDecl> variableDecls = new ListBuffer<>();
                        List<String> methodDecls = new ArrayList<>();
                        for (JCTree member : classBody.getMembers())
                        {
                            if (member instanceof JCTree.JCVariableDecl)
                            {
                                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) member;
                                variableDecls.add(jcVariableDecl);
                            }
                            else if (member instanceof JCTree.JCMethodDecl)
                            {
                                JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) member;
                                methodDecls.add(jcMethodDecl.getName().toString());
                            }
                        }
                        Getter getter = tsym.getAnnotation(Getter.class);
                        if (getter != null)
                        {
                            treeMaker.at(newClass.pos);
                            for (JCTree.JCVariableDecl variableDecl : variableDecls)
                            {
                                String name = variableDecl.getName().toString();
                                String get = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                                if(methodDecls.contains(get))continue;
                                JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(
                                        treeMaker.Modifiers(Flags.PUBLIC),
                                        names.fromString(get),
                                        variableDecl.vartype,
                                        com.sun.tools.javac.util.List.nil(),
                                        com.sun.tools.javac.util.List.nil(),
                                        com.sun.tools.javac.util.List.nil(),
                                        treeMaker.Block(0, com.sun.tools.javac.util.List.of(treeMaker.Return(treeMaker.Ident(variableDecl.getName())))),
                                        null
                                );
                                classBody.defs = classBody.defs.append(jcMethodDecl);
                            }
                        }
                        Setter setter = tsym.getAnnotation(Setter.class);
                        if (setter != null)
                        {
                            treeMaker.at(newClass.pos);
                            for (JCTree.JCVariableDecl variableDecl : variableDecls)
                            {
                                String name = variableDecl.getName().toString();
                                String set = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                                if(methodDecls.contains(set))continue;
                                JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(
                                        treeMaker.Modifiers(Flags.PUBLIC),
                                        names.fromString("set" + name.substring(0, 1).toUpperCase() + name.substring(1)),
                                        treeMaker.TypeIdent(TypeTag.VOID),
                                        com.sun.tools.javac.util.List.nil(),
                                        com.sun.tools.javac.util.List.of(
                                                treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), variableDecl.getName(), variableDecl.vartype, null)
                                        ),
                                        com.sun.tools.javac.util.List.nil(),
                                        treeMaker.Block(0, com.sun.tools.javac.util.List.of(
                                                treeMaker.Exec(
                                                        treeMaker.Assign(
                                                                treeMaker.Select(treeMaker.Ident(names._this), variableDecl.getName()),
                                                                treeMaker.Ident(variableDecl.getName())
                                                        )
                                                )
                                        )),
                                        null
                                );
                                classBody.defs = classBody.defs.append(jcMethodDecl);
                            }
                        }
                    }
                }
            });
        }
    }

    private JCTree.JCImport getImport(String simpleName, List<JCTree.JCImport> imports)
    {
        List<JCTree.JCImport> collect = imports.stream()
                .filter(i -> !i.isStatic() && ReflectUtil.<JCTree>getFieldValue(i, "qualid").toString().endsWith("." + simpleName))
                .collect(Collectors.toList());
        if (collect.size() != 1) return null;
        return collect.get(0);
    }
}
