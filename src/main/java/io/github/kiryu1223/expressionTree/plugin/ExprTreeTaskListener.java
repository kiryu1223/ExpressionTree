package io.github.kiryu1223.expressionTree.plugin;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import io.github.kiryu1223.expressionTree.expressions.Expression;
import io.github.kiryu1223.expressionTree.expressions.Kind;
import io.github.kiryu1223.expressionTree.expressions.OperatorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExprTreeTaskListener implements TaskListener
{
    private final TreeMaker treeMaker;
    private final Types types;
    private final Names names;
    private final Map<Kind, JCTree.JCFieldAccess> methods = new HashMap<>();
    private final Map<JCTree.Tag, JCTree.JCFieldAccess> operators = new HashMap<>();
    private final Map<JCTree.JCFieldAccess, JCTree> needToChangeClasses = new HashMap<>();

    public ExprTreeTaskListener(Context context)
    {
        treeMaker = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        init();
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
            codeReplace(event);
            typeReplace(event);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private void codeReplace(TaskEvent event)
    {
        if (event.getKind() != TaskEvent.Kind.PARSE) return;
        CompilationUnitTree compUnit = event.getCompilationUnit();
        List<ImportInfo> imports = new ArrayList<>(compUnit.getImports().size());
        for (ImportTree anImport : compUnit.getImports())
        {
            imports.add(new ImportInfo(anImport.getQualifiedIdentifier().toString(), anImport.isStatic()));
        }
        for (Tree typeDecl : compUnit.getTypeDecls())
        {
            if (!(typeDecl instanceof JCTree.JCClassDecl)) continue;
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) typeDecl;
            Map<String, JCTree.JCVariableDecl> classFields = new HashMap<>();
            for (JCTree member : classDecl.getMembers())
            {
                if (member.getKind() != Tree.Kind.VARIABLE) continue;
                JCTree.JCVariableDecl field = (JCTree.JCVariableDecl) member;
                classFields.put(field.getName().toString(), field);
            }
            classDecl.accept(new ExprTranslator(
                    imports,
                    treeMaker,
                    types, names,
                    classFields, needToChangeClasses, methods,
                    operators
            ));
        }
    }

    private void typeReplace(TaskEvent event)
    {
        if (event.getKind() != TaskEvent.Kind.ANALYZE) return;
        if (needToChangeClasses.isEmpty()) return;
        needToChangeClasses.forEach((k, v) ->
        {
            AtomicReference<Type> type = new AtomicReference<>();
            if (isAnonymousClass(v))
            {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) v;
                type.set(classDecl.getExtendsClause().type);
            }
            else if (v.getKind() == Tree.Kind.LAMBDA_EXPRESSION)
            {
                JCTree.JCLambda lambda = (JCTree.JCLambda) v;
                if (lambda.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION)
                {
                    type.set(lambda.getBody().type);
                }
                else
                {
                    lambda.getBody().accept(new TreeScanner()
                    {
                        @Override
                        public void visitReturn(JCTree.JCReturn jcReturn)
                        {
                            type.set(jcReturn.getExpression().type);
                            super.visitReturn(jcReturn);
                        }
                    });
                }
            }
            else
            {
                type.set(v.type);
            }
            if (type.get() != null)
            {
                k.selected.setType(type.get());
            }
        });
        needToChangeClasses.clear();
    }

    private boolean isAnonymousClass(JCTree tree)
    {
        return tree instanceof JCTree.JCClassDecl && ((JCTree.JCClassDecl) tree).getSimpleName().isEmpty();
    }
}
