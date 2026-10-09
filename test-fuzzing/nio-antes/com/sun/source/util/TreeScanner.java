package com.sun.source.util;

import com.sun.source.tree.*;

/**
 * A visitor that walks the whole syntax tree and combines what each node returns.
 *
 * <h2>What it contributes over implementing the visitor by hand</h2>
 *
 * <p>The walk. Each {@code visitXxx} here already knows which that node's children are and
 * visits them; whoever extends this class overrides only those that are of interest and calls
 * {@code super.visitXxx(node, p)} so that the rest go on going down. Without that, forgetting a
 * child in one of the 68 methods leaves a branch of the tree unwalked, and there is no error
 * that says so.
 *
 * <h2>How the results are combined</h2>
 *
 * <p>With {@link #reduce}, which by default returns the first that is not {@code null}. It
 * serves for "find the first that holds"; for accumulating -- counting, gathering into a list
 * -- it has to be overridden.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along the walk
 */
public class TreeScanner<R, P> implements TreeVisitor<R, P> {

    public TreeScanner() {
    }

    /** It visits a node, or {@code null} if there is none. */
    public R scan(Tree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** It visits every one of the list, combining what they return. */
    public R scan(Iterable<? extends Tree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            boolean first = true;
            for (Tree node : nodes) {
                r = first ? scan(node, p) : reduce(scan(node, p), r);
                first = false;
            }
        }
        return r;
    }

    private R scanAndReduce(Tree node, P p, R r) {
        return reduce(scan(node, p), r);
    }

    private R scanAndReduce(Iterable<? extends Tree> nodes, P p, R r) {
        return reduce(scan(nodes, p), r);
    }

    /**
     * It combines two results.
     *
     * <p>By default the one that is not {@code null} wins, with preference for the first. It is
     * the semantics of a "search": the walk goes on all the same, but what comes back is the
     * first find.
     */
    public R reduce(R r1, R r2) {
        return r1 != null ? r1 : r2;
    }


    public R visitAnnotatedType(AnnotatedTypeTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getUnderlyingType(), p, r);
        return r;
    }

    public R visitAnnotation(AnnotationTree node, P p) {
        R r = scan(node.getAnnotationType(), p);
        r = scanAndReduce(node.getArguments(), p, r);
        return r;
    }

    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        R r = scan(node.getTypeArguments(), p);
        r = scanAndReduce(node.getMethodSelect(), p, r);
        r = scanAndReduce(node.getArguments(), p, r);
        return r;
    }

    public R visitAssert(AssertTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getDetail(), p, r);
        return r;
    }

    public R visitAssignment(AssignmentTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    public R visitBinary(BinaryTree node, P p) {
        R r = scan(node.getLeftOperand(), p);
        r = scanAndReduce(node.getRightOperand(), p, r);
        return r;
    }

    public R visitBlock(BlockTree node, P p) {
        R r = scan(node.getStatements(), p);
        return r;
    }

    public R visitBreak(BreakTree node, P p) {
        return null;
    }

    public R visitCase(CaseTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getExpressions(), p, r);
        r = scanAndReduce(node.getLabels(), p, r);
        r = scanAndReduce(node.getGuard(), p, r);
        r = scanAndReduce(node.getStatements(), p, r);
        r = scanAndReduce(node.getBody(), p, r);
        return r;
    }

    public R visitCatch(CatchTree node, P p) {
        R r = scan(node.getParameter(), p);
        r = scanAndReduce(node.getBlock(), p, r);
        return r;
    }

    public R visitClass(ClassTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getTypeParameters(), p, r);
        r = scanAndReduce(node.getExtendsClause(), p, r);
        r = scanAndReduce(node.getImplementsClause(), p, r);
        r = scanAndReduce(node.getPermitsClause(), p, r);
        r = scanAndReduce(node.getMembers(), p, r);
        return r;
    }

    public R visitConditionalExpression(ConditionalExpressionTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getTrueExpression(), p, r);
        r = scanAndReduce(node.getFalseExpression(), p, r);
        return r;
    }

    public R visitContinue(ContinueTree node, P p) {
        return null;
    }

    public R visitDoWhileLoop(DoWhileLoopTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    public R visitErroneous(ErroneousTree node, P p) {
        R r = scan(node.getErrorTrees(), p);
        return r;
    }

    public R visitExpressionStatement(ExpressionStatementTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    public R visitForLoop(ForLoopTree node, P p) {
        R r = scan(node.getInitializer(), p);
        r = scanAndReduce(node.getCondition(), p, r);
        r = scanAndReduce(node.getUpdate(), p, r);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    public R visitIdentifier(IdentifierTree node, P p) {
        return null;
    }

    public R visitIf(IfTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getThenStatement(), p, r);
        r = scanAndReduce(node.getElseStatement(), p, r);
        return r;
    }

    public R visitImport(ImportTree node, P p) {
        R r = scan(node.getQualifiedIdentifier(), p);
        return r;
    }

    public R visitArrayAccess(ArrayAccessTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getIndex(), p, r);
        return r;
    }

    public R visitLabeledStatement(LabeledStatementTree node, P p) {
        R r = scan(node.getStatement(), p);
        return r;
    }

    public R visitLiteral(LiteralTree node, P p) {
        return null;
    }

    public R visitAnyPattern(AnyPatternTree node, P p) {
        return null;
    }

    public R visitBindingPattern(BindingPatternTree node, P p) {
        R r = scan(node.getVariable(), p);
        return r;
    }

    public R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p) {
        return null;
    }

    public R visitConstantCaseLabel(ConstantCaseLabelTree node, P p) {
        R r = scan(node.getConstantExpression(), p);
        return r;
    }

    public R visitPatternCaseLabel(PatternCaseLabelTree node, P p) {
        R r = scan(node.getPattern(), p);
        return r;
    }

    public R visitDeconstructionPattern(DeconstructionPatternTree node, P p) {
        R r = scan(node.getDeconstructor(), p);
        r = scanAndReduce(node.getNestedPatterns(), p, r);
        return r;
    }

    public R visitMethod(MethodTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getReturnType(), p, r);
        r = scanAndReduce(node.getTypeParameters(), p, r);
        r = scanAndReduce(node.getParameters(), p, r);
        r = scanAndReduce(node.getReceiverParameter(), p, r);
        r = scanAndReduce(node.getThrows(), p, r);
        r = scanAndReduce(node.getBody(), p, r);
        r = scanAndReduce(node.getDefaultValue(), p, r);
        return r;
    }

    public R visitModifiers(ModifiersTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        return r;
    }

    public R visitNewArray(NewArrayTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getDimensions(), p, r);
        r = scanAndReduce(node.getInitializers(), p, r);
        r = scanAndReduce(node.getAnnotations(), p, r);
        return r;
    }

    public R visitNewClass(NewClassTree node, P p) {
        R r = scan(node.getEnclosingExpression(), p);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        r = scanAndReduce(node.getIdentifier(), p, r);
        r = scanAndReduce(node.getArguments(), p, r);
        r = scanAndReduce(node.getClassBody(), p, r);
        return r;
    }

    public R visitLambdaExpression(LambdaExpressionTree node, P p) {
        R r = scan(node.getParameters(), p);
        r = scanAndReduce(node.getBody(), p, r);
        return r;
    }

    public R visitPackage(PackageTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getPackageName(), p, r);
        return r;
    }

    public R visitParenthesized(ParenthesizedTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitReturn(ReturnTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitMemberSelect(MemberSelectTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitMemberReference(MemberReferenceTree node, P p) {
        R r = scan(node.getQualifierExpression(), p);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        return r;
    }

    public R visitEmptyStatement(EmptyStatementTree node, P p) {
        return null;
    }

    public R visitSwitch(SwitchTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getCases(), p, r);
        return r;
    }

    public R visitSwitchExpression(SwitchExpressionTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getCases(), p, r);
        return r;
    }

    public R visitSynchronized(SynchronizedTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getBlock(), p, r);
        return r;
    }

    public R visitThrow(ThrowTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitCompilationUnit(CompilationUnitTree node, P p) {
        R r = scan(node.getModule(), p);
        r = scanAndReduce(node.getPackageAnnotations(), p, r);
        r = scanAndReduce(node.getPackageName(), p, r);
        r = scanAndReduce(node.getPackage(), p, r);
        r = scanAndReduce(node.getImports(), p, r);
        r = scanAndReduce(node.getTypeDecls(), p, r);
        return r;
    }

    public R visitTry(TryTree node, P p) {
        R r = scan(node.getBlock(), p);
        r = scanAndReduce(node.getCatches(), p, r);
        r = scanAndReduce(node.getFinallyBlock(), p, r);
        r = scanAndReduce(node.getResources(), p, r);
        return r;
    }

    public R visitParameterizedType(ParameterizedTypeTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        return r;
    }

    public R visitUnionType(UnionTypeTree node, P p) {
        R r = scan(node.getTypeAlternatives(), p);
        return r;
    }

    public R visitIntersectionType(IntersectionTypeTree node, P p) {
        R r = scan(node.getBounds(), p);
        return r;
    }

    public R visitArrayType(ArrayTypeTree node, P p) {
        R r = scan(node.getType(), p);
        return r;
    }

    public R visitTypeCast(TypeCastTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    public R visitPrimitiveType(PrimitiveTypeTree node, P p) {
        return null;
    }

    public R visitTypeParameter(TypeParameterTree node, P p) {
        R r = scan(node.getBounds(), p);
        r = scanAndReduce(node.getAnnotations(), p, r);
        return r;
    }

    public R visitInstanceOf(InstanceOfTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getType(), p, r);
        r = scanAndReduce(node.getPattern(), p, r);
        return r;
    }

    public R visitUnary(UnaryTree node, P p) {
        R r = scan(node.getExpression(), p);
        return r;
    }

    public R visitVariable(VariableTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getNameExpression(), p, r);
        r = scanAndReduce(node.getType(), p, r);
        r = scanAndReduce(node.getInitializer(), p, r);
        return r;
    }

    public R visitWhileLoop(WhileLoopTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    public R visitWildcard(WildcardTree node, P p) {
        R r = scan(node.getBound(), p);
        return r;
    }

    public R visitModule(ModuleTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getName(), p, r);
        r = scanAndReduce(node.getDirectives(), p, r);
        return r;
    }

    public R visitExports(ExportsTree node, P p) {
        R r = scan(node.getPackageName(), p);
        r = scanAndReduce(node.getModuleNames(), p, r);
        return r;
    }

    public R visitOpens(OpensTree node, P p) {
        R r = scan(node.getPackageName(), p);
        r = scanAndReduce(node.getModuleNames(), p, r);
        return r;
    }

    public R visitProvides(ProvidesTree node, P p) {
        R r = scan(node.getServiceName(), p);
        r = scanAndReduce(node.getImplementationNames(), p, r);
        return r;
    }

    public R visitRequires(RequiresTree node, P p) {
        R r = scan(node.getModuleName(), p);
        return r;
    }

    public R visitUses(UsesTree node, P p) {
        R r = scan(node.getServiceName(), p);
        return r;
    }

    public R visitOther(Tree node, P p) {
        return null;
    }

    public R visitYield(YieldTree node, P p) {
        R r = scan(node.getValue(), p);
        return r;
    }
}
