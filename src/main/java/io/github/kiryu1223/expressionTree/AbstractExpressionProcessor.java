package io.github.kiryu1223.expressionTree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.FunctionalInterface.IReturnBoolean;
import io.github.kiryu1223.expressionTree.FunctionalInterface.IReturnGeneric;
import io.github.kiryu1223.expressionTree.FunctionalInterface.IReturnVoid;
import io.github.kiryu1223.expressionTree.expressionV2.IExpression;
import io.github.kiryu1223.expressionTree.expressionV2.NewExpression;
import io.github.kiryu1223.expressionTree.info.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

import static io.github.kiryu1223.expressionTree.expressionV2.IExpression.Type.*;

public abstract class AbstractExpressionProcessor extends AbstractProcessor
{
    private JavacTrees javacTrees;
    private Context context;
    private TreeMaker treeMaker;
    private Names names;
    private final Map<IExpression.Type, JCTree.JCFieldAccess> expressionMap = new HashMap<>();
    private final Map<JCTree.Tag, JCTree.JCFieldAccess> opMap = new HashMap<>();
    Types types;
    Elements elements;
    private final List<ClassInfo> classInfos = new ArrayList<>();

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        SupportedAnnotationTypes sat = new SupportedAnnotationTypes()
        {
            @Override
            public Class<? extends Annotation> annotationType()
            {
                return SupportedAnnotationTypes.class;
            }

            @Override
            public String[] value()
            {
                return new String[]{"*"};
            }
        };
        boolean initialized = isInitialized();
        boolean stripModulePrefixes = initialized && processingEnv.getSourceVersion().compareTo(SourceVersion.RELEASE_8) <= 0;
        return arrayToSet(sat.value(), stripModulePrefixes);

    }

    private static Set<String> arrayToSet(String[] array, boolean stripModulePrefixes)
    {
        assert array != null;
        Set<String> set = new HashSet<>(array.length);
        for (String s : array)
        {
            if (stripModulePrefixes)
            {
                int index = s.indexOf('/');
                if (index != -1)
                    s = s.substring(index + 1);
            }
            set.add(s);
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        if (getClass().isAnnotationPresent(SupportedSourceVersion.class))
        {
            return getClass().getAnnotation(SupportedSourceVersion.class).value();
        }
        return SourceVersion.RELEASE_8;
    }

    private void register()
    {
        JarHelper jarHelper = new JarHelper(this.getClass());
        for (Class<?> clazz : jarHelper.getClasses())
        {
            if(clazz.isAnonymousClass())continue;
            ClassInfo classInfo = new ClassInfo(clazz.getPackage().getName(), clazz.getCanonicalName());
            if (isFunctionInterFace(clazz))
            {
                Method method = clazz.getMethods()[0];
                MethodInfo methodInfo = new MethodInfo(method.getName(), method.getReturnType().getCanonicalName());
                for (Parameter parameter : method.getParameters())
                {
                    ParamInfo paramInfo = new ParamInfo(parameter.getType().getCanonicalName());
                    methodInfo.getParamInfos().add(paramInfo);
                }
                classInfo.getMethodInfos().add(methodInfo);
            }
            else
            {
                for (Method method : clazz.getMethods())
                {
                    MethodInfo methodInfo = new MethodInfo(method.getName(), method.getReturnType().getCanonicalName());
                    for (Parameter parameter : method.getParameters())
                    {
                        ParamInfo paramInfo = null;
                        Class<?> parameterType = parameter.getType();
                        if (isFunctionInterFace(parameterType))
                        {
                            paramInfo = new ParamInfo(parameterType.getCanonicalName());
                        }
                        else
                        {
                            paramInfo = new ParamInfo(parameterType.getCanonicalName());
                        }
                        if (parameter.isAnnotationPresent(Expression.class))
                        {
                            AnnoInfo annoInfo = new AnnoInfo(Expression.class.getCanonicalName(), parameter.getAnnotation(Expression.class).value());
                            paramInfo.getAnnoInfo().add(annoInfo);
                        }
                        methodInfo.getParamInfos().add(paramInfo);
                    }
                    classInfo.getMethodInfos().add(methodInfo);
                }
            }
            classInfos.add(classInfo);
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        javacTrees = JavacTrees.instance(processingEnv);
        context = ((JavacProcessingEnvironment) processingEnv).getContext();
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        register();
        String expV2 = "io.github.kiryu1223.expressionTree.expressionV2";

        JCTree.JCFieldAccess operator = treeMaker.Select(treeMaker.Ident(names.fromString(expV2)), names.fromString("Operator"));
        opMap.put(JCTree.Tag.EQ, treeMaker.Select(operator, names.fromString("EQ")));
        opMap.put(JCTree.Tag.NE, treeMaker.Select(operator, names.fromString("NE")));
        opMap.put(JCTree.Tag.GE, treeMaker.Select(operator, names.fromString("GE")));
        opMap.put(JCTree.Tag.LE, treeMaker.Select(operator, names.fromString("LE")));
        opMap.put(JCTree.Tag.GT, treeMaker.Select(operator, names.fromString("GT")));
        opMap.put(JCTree.Tag.LT, treeMaker.Select(operator, names.fromString("LT")));
        opMap.put(JCTree.Tag.AND, treeMaker.Select(operator, names.fromString("And")));
        opMap.put(JCTree.Tag.OR, treeMaker.Select(operator, names.fromString("Or")));
        opMap.put(JCTree.Tag.NOT, treeMaker.Select(operator, names.fromString("NOT")));
        opMap.put(JCTree.Tag.PLUS, treeMaker.Select(operator, names.fromString("PLUS")));
        opMap.put(JCTree.Tag.MINUS, treeMaker.Select(operator, names.fromString("MINUS")));
        opMap.put(JCTree.Tag.MUL, treeMaker.Select(operator, names.fromString("MUL")));
        opMap.put(JCTree.Tag.DIV, treeMaker.Select(operator, names.fromString("DIV")));
        opMap.put(JCTree.Tag.MOD, treeMaker.Select(operator, names.fromString("MOD")));

        JCTree.JCFieldAccess iExpression = treeMaker.Select(treeMaker.Ident(names.fromString(expV2)), names.fromString("IExpression"));
        expressionMap.put(Binary, treeMaker.Select(iExpression, names.fromString(Binary.getMethodName())));
        expressionMap.put(Value, treeMaker.Select(iExpression, names.fromString(Value.getMethodName())));
        expressionMap.put(Unary, treeMaker.Select(iExpression, names.fromString(Unary.getMethodName())));
        expressionMap.put(New, treeMaker.Select(iExpression, names.fromString(New.getMethodName())));
        expressionMap.put(Parens, treeMaker.Select(iExpression, names.fromString(Parens.getMethodName())));
        expressionMap.put(FieldSelect, treeMaker.Select(iExpression, names.fromString(FieldSelect.getMethodName())));
        expressionMap.put(MethodCall, treeMaker.Select(iExpression, names.fromString(MethodCall.getMethodName())));
        expressionMap.put(Reference, treeMaker.Select(iExpression, names.fromString(Reference.getMethodName())));
        expressionMap.put(Block, treeMaker.Select(iExpression, names.fromString(Block.getMethodName())));
        expressionMap.put(Assign, treeMaker.Select(iExpression, names.fromString(Assign.getMethodName())));
        expressionMap.put(Var, treeMaker.Select(iExpression, names.fromString(Var.getMethodName())));
        expressionMap.put(ArrayAccess, treeMaker.Select(iExpression, names.fromString(ArrayAccess.getMethodName())));
        expressionMap.put(If, treeMaker.Select(iExpression, names.fromString(If.getMethodName())));
        expressionMap.put(LocalReference, treeMaker.Select(iExpression, names.fromString(LocalReference.getMethodName())));
        expressionMap.put(Return, treeMaker.Select(iExpression, names.fromString(Return.getMethodName())));
        expressionMap.put(Break, treeMaker.Select(iExpression, names.fromString(Break.getMethodName())));
        expressionMap.put(Continue, treeMaker.Select(iExpression, names.fromString(Continue.getMethodName())));
        expressionMap.put(Switch, treeMaker.Select(iExpression, names.fromString(Switch.getMethodName())));
        expressionMap.put(Case, treeMaker.Select(iExpression, names.fromString(Case.getMethodName())));
        expressionMap.put(Conditional, treeMaker.Select(iExpression, names.fromString(Conditional.getMethodName())));
        expressionMap.put(Foreach, treeMaker.Select(iExpression, names.fromString(Foreach.getMethodName())));
        expressionMap.put(While, treeMaker.Select(iExpression, names.fromString(While.getMethodName())));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (!roundEnv.processingOver())
        {
            Set<? extends Element> roots = roundEnv.getRootElements();
            String ExpressionAll = "io.github.kiryu1223.expressionTree.*";
            String ExpressionAnno = "io.github.kiryu1223.expressionTree.Expression";

            //获取全部类
            List<ClassInfo> classInfoList = new ArrayList<>(roots.size());
            for (Element root : roots)
            {
                TreePath path = javacTrees.getPath(root);
                ExpressionTree packageName = path.getCompilationUnit().getPackageName();
                ClassInfo classInfo = new ClassInfo(packageName.toString(), root.toString());
                List<? extends ImportTree> importTrees = path.getCompilationUnit().getImports();
                for (ImportTree importTree : importTrees)
                {
                    JCTree.JCImport jcImport = (JCTree.JCImport) importTree;
                    classInfo.getImportInfos().add(
                            new ImportInfo(
                                    jcImport.getQualifiedIdentifier().toString(),
                                    jcImport.isStatic()
                            )
                    );
                }
                classInfoList.add(classInfo);
            }
            //头插使其顺序正确
            classInfos.addAll(0, classInfoList);

            //给全部类获取方法
            int index = 0;
            for (Element root : roots)
            {
                ClassInfo currentClassInfo = classInfos.get(index++);
                TreePath path = javacTrees.getPath(root);
                for (JCTree member : javacTrees.getTree((TypeElement) root).getMembers())
                {
                    if (member instanceof JCTree.JCMethodDecl)
                    {
                        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) member;
                        if (methodDecl.getReturnType() == null || methodDecl.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.PRIVATE))
                            continue;
                        JCTree returnType = methodDecl.getReturnType();
                        String returnStr = returnType.toString();
                        if (returnType instanceof JCTree.JCTypeApply)
                        {
                            returnStr = ((JCTree.JCTypeApply) returnType).getType().toString();
                        }
                        returnStr = findFullName(returnStr, currentClassInfo);
                        MethodInfo methodInfo = new MethodInfo(methodDecl.getName().toString(), returnStr);
                        for (JCTree.JCVariableDecl parameter : methodDecl.getParameters())
                        {
                            JCTree parameterType = parameter.getType();
                            String parameterStr = parameterType.toString();
                            if (parameterType instanceof JCTree.JCTypeApply)
                            {
                                JCTree.JCTypeApply typeApply = (JCTree.JCTypeApply) parameterType;
                                parameterStr = typeApply.getType().toString();
                            }
                            parameterStr = findFullName(parameterStr, currentClassInfo);
                            ParamInfo paramInfo = new ParamInfo(parameterStr);
                            com.sun.tools.javac.util.List<JCTree.JCAnnotation> annotationList = parameter.getModifiers().getAnnotations();
                            for (JCTree.JCAnnotation annotation : annotationList)
                            {
                                if (annotation.getAnnotationType() instanceof JCTree.JCIdent
                                        && ((JCTree.JCIdent) annotation.getAnnotationType()).getName().toString().equals("Expression")
                                        && (currentClassInfo.containsImport(ExpressionAnno) || currentClassInfo.containsImport(ExpressionAll)))
                                {
                                    Class<? extends IExpression> cc = IExpression.class;
                                    if (!annotation.getArguments().isEmpty())
                                    {
                                        for (JCTree.JCExpression argument : annotation.getArguments())
                                        {
                                            JCTree.JCAssign assign = (JCTree.JCAssign) argument;
                                            if (assign.getVariable().toString().equals("value"))
                                            {
                                                switch (assign.getExpression().toString())
                                                {
                                                    case "NewExpression.class":
                                                        cc = NewExpression.class;
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                    paramInfo.getAnnoInfo().add(new AnnoInfo(ExpressionAnno, cc));
                                    break;
                                }
                            }
                            methodInfo.getParamInfos().add(paramInfo);
                        }
                        currentClassInfo.getMethodInfos().add(methodInfo);
                    }
                }
            }

            //表达式树流程
            index = 0;
            for (Element root : roots)
            {
                ClassInfo currentClassInfo = classInfos.get(index++);
                if (!root.getKind().equals(ElementKind.CLASS)) continue;
                TreePath path = javacTrees.getPath(root);
                JCTree.JCClassDecl jcClassDecl = javacTrees.getTree((TypeElement) root);
                treeMaker.pos = jcClassDecl.getPreferredPosition();
                List<VarInfo> varInfos = new ArrayList<>();
                for (JCTree member : jcClassDecl.getMembers())
                {
                    if (member instanceof JCTree.JCVariableDecl)
                    {
                        JCTree.JCVariableDecl variable = (JCTree.JCVariableDecl) member;
                        String typeName = findFullName(variable.getType().toString(), currentClassInfo);
                        varInfos.add(new VarInfo(0, variable.getName().toString(), typeName));
                    }
                }
                jcClassDecl.accept(new ExpressionTranslator(
                        varInfos, classInfos,
                        opMap, expressionMap, treeMaker,
                        names, currentClassInfo
                ));
            }
        }
        return false;
    }

    private String findFullName(String typeName, ClassInfo currentClassInfo)
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

    private int getParamCount(String typeName)
    {
        int count = 0;
        for (ClassInfo classInfo : classInfos)
        {
            if (classInfo.getFullName().equals(typeName))
            {
                count = classInfo.getMethodInfos().get(0).getParamInfos().size();
                break;
            }
        }
        return count;
    }

    private boolean isFunctionInterFace(Class<?> type)
    {
        if (!type.isInterface()) return false;
        return type.isAnnotationPresent(FunctionalInterface.class);
    }

    private int getFunctionParamCount(Class<?> type)
    {
        int count = 0;
        for (Method method : type.getMethods())
        {
            int modifiers = method.getModifiers();
            if (method.isDefault() || modifiers == Modifier.STRICT
                    || modifiers == (Modifier.PUBLIC + Modifier.STRICT)
                    || method.getParameterCount() == 0)
            {
                continue;
            }
            count = method.getParameterCount();
            break;
        }
        return count;
    }
}
