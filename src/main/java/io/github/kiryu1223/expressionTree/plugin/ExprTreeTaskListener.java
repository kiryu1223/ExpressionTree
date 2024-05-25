package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.Expression;
import io.github.kiryu1223.expressionTree.expressions.Kind;
import io.github.kiryu1223.expressionTree.expressions.OperatorType;
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
    private final Context context;
    private final Map<Kind, JCTree.JCFieldAccess> methods = new HashMap<>();
    private final Map<JCTree.Tag, JCTree.JCFieldAccess> operators = new HashMap<>();
    private final Map<String, Map<JCTree.JCFieldAccess, JCTree>> needToChangeClasses = new HashMap<>();
    private final Map<String, List<JCTree.JCMethodInvocation>> needToChangeRef = new HashMap<>();

    public ExprTreeTaskListener(Context context)
    {
        treeMaker = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        this.context = context;
        //init();
    }

    public void init()
    {
        String path = Expression.class.getPackage().getName();
        String expr = Expression.class.getSimpleName();

        JCTree.JCFieldAccess expression = treeMaker.Select(treeMaker.Ident(names.fromString(path)), names.fromString(expr));
        methods.put(Kind.Assign, treeMaker.Select(expression, names.fromString(Kind.Assign.name())));
        methods.put(Kind.AssignOp, treeMaker.Select(expression, names.fromString(Kind.AssignOp.name())));
        methods.put(Kind.Binary, treeMaker.Select(expression, names.fromString(Kind.Binary.name())));
        methods.put(Kind.Block, treeMaker.Select(expression, names.fromString(Kind.Block.name())));
        methods.put(Kind.Constant, treeMaker.Select(expression, names.fromString(Kind.Constant.name())));
        methods.put(Kind.FieldSelect, treeMaker.Select(expression, names.fromString(Kind.FieldSelect.name())));
        methods.put(Kind.Index, treeMaker.Select(expression, names.fromString(Kind.Index.name())));
        methods.put(Kind.Lambda, treeMaker.Select(expression, names.fromString(Kind.Lambda.name())));
        methods.put(Kind.MethodCall, treeMaker.Select(expression, names.fromString(Kind.MethodCall.name())));
        methods.put(Kind.NewArray, treeMaker.Select(expression, names.fromString(Kind.NewArray.name())));
        methods.put(Kind.New, treeMaker.Select(expression, names.fromString(Kind.New.name())));
        methods.put(Kind.Parameter, treeMaker.Select(expression, names.fromString(Kind.Parameter.name())));
        methods.put(Kind.Unary, treeMaker.Select(expression, names.fromString(Kind.Unary.name())));
        methods.put(Kind.Variable, treeMaker.Select(expression, names.fromString(Kind.Variable.name())));
        methods.put(Kind.StaticClass, treeMaker.Select(expression, names.fromString(Kind.StaticClass.name())));
        methods.put(Kind.Reference, treeMaker.Select(expression, names.fromString(Kind.Reference.name())));
        methods.put(Kind.Return, treeMaker.Select(expression, names.fromString(Kind.Return.name())));
        methods.put(Kind.Break, treeMaker.Select(expression, names.fromString(Kind.Break.name())));
        methods.put(Kind.Continue, treeMaker.Select(expression, names.fromString(Kind.Continue.name())));
        methods.put(Kind.Conditional, treeMaker.Select(expression, names.fromString(Kind.Conditional.name())));
        methods.put(Kind.If, treeMaker.Select(expression, names.fromString(Kind.If.name())));
        methods.put(Kind.Parens, treeMaker.Select(expression, names.fromString(Kind.Parens.name())));
        methods.put(Kind.Foreach, treeMaker.Select(expression, names.fromString(Kind.Foreach.name())));
        methods.put(Kind.For, treeMaker.Select(expression, names.fromString(Kind.For.name())));
        methods.put(Kind.While, treeMaker.Select(expression, names.fromString(Kind.While.name())));
        methods.put(Kind.Switch, treeMaker.Select(expression, names.fromString(Kind.Switch.name())));
        methods.put(Kind.Case, treeMaker.Select(expression, names.fromString(Kind.Case.name())));
        methods.put(Kind.Catch, treeMaker.Select(expression, names.fromString(Kind.Catch.name())));
        methods.put(Kind.Try, treeMaker.Select(expression, names.fromString(Kind.Try.name())));
        methods.put(Kind.Throw, treeMaker.Select(expression, names.fromString(Kind.Throw.name())));
        methods.put(Kind.TypeCast, treeMaker.Select(expression, names.fromString(Kind.TypeCast.name())));

        String opPath = OperatorType.class.getPackage().getName();
        String opName = OperatorType.class.getSimpleName();

        JCTree.JCFieldAccess operator = treeMaker.Select(treeMaker.Ident(names.fromString(opPath)), names.fromString(opName));
        operators.put(JCTree.Tag.POS, treeMaker.Select(operator, names.fromString(OperatorType.POS.name())));
        operators.put(JCTree.Tag.NEG, treeMaker.Select(operator, names.fromString(OperatorType.NEG.name())));
        operators.put(JCTree.Tag.NOT, treeMaker.Select(operator, names.fromString(OperatorType.NOT.name())));
        operators.put(JCTree.Tag.COMPL, treeMaker.Select(operator, names.fromString(OperatorType.COMPL.name())));
        operators.put(JCTree.Tag.PREINC, treeMaker.Select(operator, names.fromString(OperatorType.PREINC.name())));
        operators.put(JCTree.Tag.PREDEC, treeMaker.Select(operator, names.fromString(OperatorType.PREDEC.name())));
        operators.put(JCTree.Tag.POSTINC, treeMaker.Select(operator, names.fromString(OperatorType.POSTINC.name())));
        operators.put(JCTree.Tag.POSTDEC, treeMaker.Select(operator, names.fromString(OperatorType.POSTDEC.name())));
        operators.put(JCTree.Tag.NULLCHK, treeMaker.Select(operator, names.fromString(OperatorType.NULLCHK.name())));
        operators.put(JCTree.Tag.OR, treeMaker.Select(operator, names.fromString(OperatorType.OR.name())));
        operators.put(JCTree.Tag.AND, treeMaker.Select(operator, names.fromString(OperatorType.AND.name())));
        operators.put(JCTree.Tag.BITOR, treeMaker.Select(operator, names.fromString(OperatorType.BITOR.name())));
        operators.put(JCTree.Tag.BITXOR, treeMaker.Select(operator, names.fromString(OperatorType.BITXOR.name())));
        operators.put(JCTree.Tag.BITAND, treeMaker.Select(operator, names.fromString(OperatorType.BITAND.name())));
        operators.put(JCTree.Tag.EQ, treeMaker.Select(operator, names.fromString(OperatorType.EQ.name())));
        operators.put(JCTree.Tag.NE, treeMaker.Select(operator, names.fromString(OperatorType.NE.name())));
        operators.put(JCTree.Tag.LT, treeMaker.Select(operator, names.fromString(OperatorType.LT.name())));
        operators.put(JCTree.Tag.GT, treeMaker.Select(operator, names.fromString(OperatorType.GT.name())));
        operators.put(JCTree.Tag.LE, treeMaker.Select(operator, names.fromString(OperatorType.LE.name())));
        operators.put(JCTree.Tag.GE, treeMaker.Select(operator, names.fromString(OperatorType.GE.name())));
        operators.put(JCTree.Tag.SL, treeMaker.Select(operator, names.fromString(OperatorType.SL.name())));
        operators.put(JCTree.Tag.SR, treeMaker.Select(operator, names.fromString(OperatorType.SR.name())));
        operators.put(JCTree.Tag.USR, treeMaker.Select(operator, names.fromString(OperatorType.USR.name())));
        operators.put(JCTree.Tag.PLUS, treeMaker.Select(operator, names.fromString(OperatorType.PLUS.name())));
        operators.put(JCTree.Tag.MINUS, treeMaker.Select(operator, names.fromString(OperatorType.MINUS.name())));
        operators.put(JCTree.Tag.MUL, treeMaker.Select(operator, names.fromString(OperatorType.MUL.name())));
        operators.put(JCTree.Tag.DIV, treeMaker.Select(operator, names.fromString(OperatorType.DIV.name())));
        operators.put(JCTree.Tag.MOD, treeMaker.Select(operator, names.fromString(OperatorType.MOD.name())));
        operators.put(JCTree.Tag.BITOR_ASG, treeMaker.Select(operator, names.fromString(OperatorType.BITOR_ASG.name())));
        operators.put(JCTree.Tag.BITXOR_ASG, treeMaker.Select(operator, names.fromString(OperatorType.BITXOR_ASG.name())));
        operators.put(JCTree.Tag.BITAND_ASG, treeMaker.Select(operator, names.fromString(OperatorType.BITAND_ASG.name())));
        operators.put(JCTree.Tag.SL_ASG, treeMaker.Select(operator, names.fromString(OperatorType.SL_ASG.name())));
        operators.put(JCTree.Tag.SR_ASG, treeMaker.Select(operator, names.fromString(OperatorType.SR_ASG.name())));
        operators.put(JCTree.Tag.USR_ASG, treeMaker.Select(operator, names.fromString(OperatorType.USR_ASG.name())));
        operators.put(JCTree.Tag.PLUS_ASG, treeMaker.Select(operator, names.fromString(OperatorType.PLUS_ASG.name())));
        operators.put(JCTree.Tag.MINUS_ASG, treeMaker.Select(operator, names.fromString(OperatorType.MINUS_ASG.name())));
        operators.put(JCTree.Tag.MUL_ASG, treeMaker.Select(operator, names.fromString(OperatorType.MUL_ASG.name())));
        operators.put(JCTree.Tag.DIV_ASG, treeMaker.Select(operator, names.fromString(OperatorType.DIV_ASG.name())));
        operators.put(JCTree.Tag.MOD_ASG, treeMaker.Select(operator, names.fromString(OperatorType.MOD_ASG.name())));
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
            classDecl.accept(JDK.is9orLater()
                    ? new SugarScanner(thiz, context, moduleSymbol)
                    : new SugarScanner(thiz, context));
//            Symtab symtab = Symtab.instance(context);
//            classDecl.accept(new TreeTranslator()
//            {
//                @Override
//                public void visitNewClass(JCTree.JCNewClass newClass)
//                {
//                    if (newClass.getClassBody() != null)
//                    {
//                        treeMaker.at(newClass.pos);
//                        JCTree.JCClassDecl classBody = newClass.getClassBody();
//                        Getter getter = newClass.getIdentifier().type.tsym.getAnnotation(Getter.class);
//                        if (getter != null)
//                        {
//                            Symbol.ClassSymbol curOwner = classBody.sym;
//                            ListBuffer<JCTree> treeData = new ListBuffer<>();
//                            ListBuffer<JCTree.JCVariableDecl> variableDecls = new ListBuffer<>();
//                            JCTree.JCMethodDecl check = null;
//                            for (JCTree member : classBody.getMembers())
//                            {
//                                treeData.append(member);
//                                if (member instanceof JCTree.JCVariableDecl)
//                                {
//                                    variableDecls.append(((JCTree.JCVariableDecl) member));
//                                }
//                                else if (member instanceof JCTree.JCMethodDecl)
//                                {
//                                    JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) member;
//                                    if (jcMethodDecl.getName().toString().equals("getId"))
//                                    {
//                                        jcMethodDecl.name = names.fromString("getIds");
//                                    }
//                                }
//                            }
//                            System.out.println(newClass);
//                            result = newClass;
////                            for (JCTree.JCVariableDecl variableDecl : variableDecls)
////                            {
////                                String name = variableDecl.getName().toString();
////                                Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
////                                        Flags.PUBLIC,
////                                        names.fromString("get" + name.substring(0, 1).toUpperCase() + name.substring(1)),
////                                        new Type.MethodType(
////                                                com.sun.tools.javac.util.List.nil(),
////                                                variableDecl.getType().type,
////                                                com.sun.tools.javac.util.List.nil(),
////                                                symtab.methodClass
////                                        ),
////                                        curOwner
////                                );
////                                JCTree.JCMethodDecl getterMethod = treeMaker.MethodDef(
////                                        methodSymbol,
////                                        treeMaker.Block(
////                                                0,
////                                                com.sun.tools.javac.util.List.of(treeMaker.Return(treeMaker.Ident(variableDecl)))
////                                        )
////                                );
////
//////                                if (check != null)
//////                                {
//////                                    System.out.println(getterMethod.getName());
//////                                    System.out.println(check.getName());
//////                                    System.out.println(getterMethod.getName().equals(check.getName()));
//////                                    System.out.println();
//////                                    System.out.println(getterMethod.getBody());
//////                                    System.out.println(check.getBody());
//////                                    System.out.println(getterMethod.getBody().equals(check.getBody()));
//////                                    System.out.println();
//////                                    System.out.println(getterMethod.getModifiers());
//////                                    System.out.println(check.getModifiers());
//////                                    System.out.println(getterMethod.getModifiers().flags==check.getModifiers().flags);
//////                                    System.out.println();
//////                                    System.out.println(getterMethod.getTag());
//////                                    System.out.println(check.getTag());
//////                                    System.out.println(getterMethod.getTag().equals(check.getTag()));
//////                                }
////
////                                treeData.append(getterMethod);
////                            }
////                            JCTree.JCClassDecl anonymoused = treeMaker.AnonymousClassDef(treeMaker.Modifiers(0), treeData.toList());
////                            anonymoused.type = classBody.type;
////                            anonymoused.sym = classBody.sym;
////                            JCTree.JCNewClass jcNewClass = treeMaker.NewClass(newClass.encl, newClass.typeargs, newClass.clazz, newClass.args, anonymoused);
////                            jcNewClass.type = newClass.type;
////                            jcNewClass.constructor = newClass.constructor;
////                            jcNewClass.varargsElement = newClass.varargsElement;
////                            jcNewClass.pos = newClass.pos;
////                            super.visitNewClass(jcNewClass);
//
//                        }
//                        else
//                        {
//                            super.visitNewClass(newClass);
//                        }
//                    }
//                    else
//                    {
//                        super.visitNewClass(newClass);
//                    }
//                }
//            });
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
        Symtab symtab = Symtab.instance(context);
//        ListBuffer<Type> hasGetterTypes = new ListBuffer<>();
//        ListBuffer<Type> hasSetterTypes = new ListBuffer<>();
//        for (Symbol.ClassSymbol classSymbol : symtab.classes.values())
//        {
//            if (classSymbol.hasAnnotations())
//            {
//                Getter getter = classSymbol.getAnnotation(Getter.class);
//                if (getter != null)
//                {
//                    hasGetterTypes.append(classSymbol.type);
//                }
//                Setter setter = classSymbol.getAnnotation(Setter.class);
//                if (setter != null)
//                {
//                    hasSetterTypes.append(classSymbol.type);
//                }
//            }
//        }
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
                        Type type = anImport.getQualifiedIdentifier().type;
                        if (type == null) return;
                        Symbol.TypeSymbol tsym = type.tsym;
                        if (tsym == null) return;
                        JCTree.JCClassDecl classBody = newClass.getClassBody();
                        ListBuffer<JCTree.JCVariableDecl> variableDecls = new ListBuffer<>();
                        for (JCTree member : classBody.getMembers())
                        {
                            if (member instanceof JCTree.JCVariableDecl)
                            {
                                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) member;
                                variableDecls.add(jcVariableDecl);
                            }
                        }
                        Getter getter = tsym.getAnnotation(Getter.class);
                        if (getter != null)
                        {
                            treeMaker.at(newClass.pos);
                            for (JCTree.JCVariableDecl variableDecl : variableDecls)
                            {
                                String name = variableDecl.getName().toString();
                                JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(
                                        treeMaker.Modifiers(Flags.PUBLIC),
                                        names.fromString("get" + name.substring(0, 1).toUpperCase() + name.substring(1)),
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
                                System.out.println(jcMethodDecl);
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
        List<JCTree.JCImport> collect = imports.stream().filter(i -> !i.isStatic() && i.getQualifiedIdentifier().toString().endsWith("." + simpleName)).collect(Collectors.toList());
        if (collect.size() != 1) return null;
        return collect.get(0);
    }
}
