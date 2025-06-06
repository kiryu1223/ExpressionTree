package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.annos.Getter;
import io.github.kiryu1223.expressionTree.expressions.annos.Setter;
import io.github.kiryu1223.expressionTree.expressions.annos.Where;
import io.github.kiryu1223.expressionTree.ext.IExtensionService;
import io.github.kiryu1223.expressionTree.util.JDK;
import io.github.kiryu1223.expressionTree.util.ReflectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ExprTreeTaskListener implements TaskListener {
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Context context;
    private final Symtab symtab;
    //private final JavaCompiler javaCompiler;
    private final ClassReader classReader;
    private final List<IExtensionService> extensionServices;
    //private final String[] compilerArgs;

    public ExprTreeTaskListener(Context context) {
        treeMaker = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
        classReader = ClassReader.instance(context);
        //javaCompiler = JavaCompiler.instance(context);
        //classReader = ClassReader.instance(context);
        //this.context = context;
        this.context = context;
        this.extensionServices = registrarExtensionService(context);
        //this.compilerArgs = compilerArgs;
    }

    // 注册我的服务
    private List<IExtensionService> registrarExtensionService(Context context) {
        ServiceLoader<IExtensionService> load = ServiceLoader.load(IExtensionService.class, ExprTreeTaskListener.class.getClassLoader());
        List<IExtensionService> iExtensionServices = new ArrayList<>();
        for (IExtensionService iExtensionService : load) {
            iExtensionService.init(context);
            iExtensionServices.add(iExtensionService);
        }
        return iExtensionServices;
    }

    private void startedCallExtensionServices(TaskEvent event) throws Throwable {
        for (IExtensionService extensionService : extensionServices) {
            extensionService.started(event);
        }
    }

    private void finishedCallExtensionServices(TaskEvent event) throws Throwable {
        for (IExtensionService extensionService : extensionServices) {
            extensionService.finished(event);
        }
    }

    @Override
    public void started(TaskEvent event) {
        //System.out.printf("%s 开始 %n", event.getKind());
        try {
            startedCallExtensionServices(event);
//            if (event.getKind() == TaskEvent.Kind.ANALYZE)
//            {
//                JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
//                for (JCTree typeDecl : compilationUnit.getTypeDecls())
//                {
//                    if (!(typeDecl instanceof JCTree.JCClassDecl)) continue;
//                    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) typeDecl;
//                    checkWhere(classDecl);
//                }
//            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finished(TaskEvent event) {
        //System.out.printf("%s 结束 %n", event.getKind());
        try {
            getterOrSetter(event);
            blockTaskMake(event);
            lambdaToTree(event);
            finishedCallExtensionServices(event);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void lambdaToTree(TaskEvent event) {
        if (event.getKind() != TaskEvent.Kind.ANALYZE) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        Object moduleSymbol = null;
        if (JDK.is9orLater()) {
            moduleSymbol = ReflectUtil.getFieldValue(compilationUnit, "modle");
        }
        for (JCTree tree : compilationUnit.getTypeDecls()) {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            //SugarScannerV2.resetIndex();
            classDecl.accept(new LambdaTrans(treeMaker, types, names, symtab, classReader, moduleSymbol));
            //classDecl.accept(new LambdaFinder(treeMaker, types, names, symtab, classReader, moduleSymbol));
            //classDecl.accept(new LambdaTreeScanner(treeMaker, types, names, symtab, classReader, moduleSymbol));
            //classDecl.accept(new SugarScannerV2(treeMaker, types, names, symtab, classReader, moduleSymbol));
            //classDecl.accept(new SugarScanner(thiz, treeMaker, types, names, symtab, javaCompiler,classReader, moduleSymbol));
        }
    }

    private void blockTaskMake(TaskEvent event) {
        if (event.getKind() != TaskEvent.Kind.PARSE) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        for (JCTree tree : compilationUnit.getTypeDecls()) {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            classDecl.accept(new TreeScanner() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl tree) {

                }

                @Override
                public void visitBlock(JCTree.JCBlock block) {
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
            });
        }
    }

    private void getterOrSetter(TaskEvent event) {
        if (event.getKind() != TaskEvent.Kind.ENTER) return;
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) event.getCompilationUnit();
        com.sun.tools.javac.util.List<JCTree.JCImport> imports = compilationUnit.getImports();
        for (JCTree tree : compilationUnit.getTypeDecls()) {
            if (!(tree instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
            classDecl.accept(new TreeScanner() {
                @Override
                public void visitNewClass(JCTree.JCNewClass newClass) {
                    if (newClass.getClassBody() != null) {
                        Type type = getTypeImport(newClass.clazz.toString(), imports);
                        if (type == null) return;
                        Symbol.TypeSymbol tsym = type.tsym;
                        if (tsym == null) return;
                        JCTree.JCClassDecl classBody = newClass.getClassBody();
                        ListBuffer<JCTree.JCVariableDecl> variableDecls = new ListBuffer<>();
                        List<String> methodDecls = new ArrayList<>();
                        for (JCTree member : classBody.getMembers()) {
                            if (member instanceof JCTree.JCVariableDecl) {
                                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) member;
                                variableDecls.add(jcVariableDecl);
                            }
                            else if (member instanceof JCTree.JCMethodDecl) {
                                JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) member;
                                methodDecls.add(jcMethodDecl.getName().toString());
                            }
                        }
                        Getter getter = tsym.getAnnotation(Getter.class);
                        if (getter != null) {
                            treeMaker.at(newClass.pos);
                            for (JCTree.JCVariableDecl variableDecl : variableDecls) {
                                String name = variableDecl.getName().toString();
                                String get = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                                if (methodDecls.contains(get)) continue;
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
                        if (setter != null) {
                            treeMaker.at(newClass.pos);
                            for (JCTree.JCVariableDecl variableDecl : variableDecls) {
                                String name = variableDecl.getName().toString();
                                String set = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                                if (methodDecls.contains(set)) continue;
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

    private Type getTypeImport(String simpleName, List<JCTree.JCImport> imports) {
        List<JCTree> qualids = new ArrayList<>();
        for (JCTree.JCImport i : imports) {
            JCTree qualid = ReflectUtil.getFieldValue(i, "qualid");
            if (!i.isStatic() && qualid.toString().endsWith("." + simpleName)) {
                return qualid.type;
            }
        }

        for (JCTree.JCImport anImport : imports) {
            JCTree qualid = ReflectUtil.getFieldValue(anImport, "qualid");
            if (!anImport.isStatic() && qualid.type == null) {
                JCTree.JCFieldAccess jcf = (JCTree.JCFieldAccess) qualid;
                if (jcf.selected instanceof JCTree.JCFieldAccess) {
                    JCTree.JCFieldAccess selected = (JCTree.JCFieldAccess) jcf.selected;
                    Symbol.PackageSymbol packageSymbol = (Symbol.PackageSymbol) selected.sym;
                    Name packageName = packageSymbol.getQualifiedName();
                    String fullName = packageName + "." + simpleName;
                    Scope members = packageSymbol.members();
                    for (Symbol element : members.getElements(m -> m.getQualifiedName().toString().equals(fullName))) {
                        return element.type;
                    }
                }
            }
        }

        return null;
    }

    private void checkWhere(JCTree.JCClassDecl classDecl) {
        JCTree.JCExpression extendsClause = classDecl.getExtendsClause();
        List<JCTree.JCExpression> implementsClause = classDecl.getImplementsClause();
        for (JCTree.JCExpression jcExpression : implementsClause) {
            if (jcExpression instanceof JCTree.JCTypeApply) {
                JCTree.JCTypeApply typeApply = (JCTree.JCTypeApply) jcExpression;
                List<JCTree.JCExpression> typeArguments = typeApply.getTypeArguments();
                JCTree.JCIdent type = (JCTree.JCIdent) typeApply.getType();
                Symbol.ClassSymbol applySym = (Symbol.ClassSymbol) type.sym;
                List<Symbol.TypeVariableSymbol> typeParameters = applySym.getTypeParameters();
                for (int i = 0; i < typeParameters.size(); i++) {
                    Symbol.TypeVariableSymbol typeParameter = typeParameters.get(i);
                    Where where = typeParameter.getAnnotation(Where.class);
                    if (where != null) {
                        io.github.kiryu1223.expressionTree.expressions.annos.Types value = where.value();
                        JCTree.JCExpression typeA = typeArguments.get(i);
                        Symbol.ClassSymbol symbol;
                        if (typeA instanceof JCTree.JCIdent) {
                            JCTree.JCIdent a = (JCTree.JCIdent) typeA;
                            symbol = (Symbol.ClassSymbol) a.sym;
                        }
                        else {
                            JCTree.JCTypeApply a = (JCTree.JCTypeApply) typeA;
                            symbol = (Symbol.ClassSymbol) ((JCTree.JCIdent) a.getType()).sym;
                        }
                        switch (value) {
                            case Class:
                                if (symbol.isInterface()) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "%s的泛型约束为%s,却获得了%s",
                                                    applySym.asType(),
                                                    "class",
                                                    typeApply
                                            )
                                    );
                                }
                                break;
                            case Interface:
                                if (!symbol.isInterface()) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "%s的泛型约束为%s,却获得了%s",
                                                    applySym,
                                                    "interface",
                                                    typeApply
                                            )
                                    );
                                }
                                break;
                        }
                    }
                }
//                for (JCTree.JCExpression typeArgument : typeApply.getTypeArguments())
//                {
//                    if (typeArgument instanceof JCTree.JCIdent)
//                    {
//                        JCTree.JCIdent argument = (JCTree.JCIdent) typeArgument;
//                        Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) argument.sym;
//
//                    }
//                }
            }
        }
    }
}
