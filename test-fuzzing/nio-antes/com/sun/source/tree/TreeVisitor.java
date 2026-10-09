package com.sun.source.tree;

/**
 * A visitor of the syntax tree.
 *
 * <h2>What it contributes over a {@code switch}</h2>
 *
 * <p>That the compiler should count. If the JDK adds a kind of node -- and it does so in every
 * version that adds syntax: the patterns, the records, the modules -- an implementation of this
 * interface stops compiling until somebody decides what to do with the new node. A
 * {@code switch} over {@link Tree.Kind} keeps quiet and falls into its default branch.
 *
 * <h2>All abstract, unlike {@link com.sun.source.doctree.DocTreeVisitor}</h2>
 *
 * <p>And the difference is a decision, not an oversight on one side or the other. The
 * documentation tree privileges not breaking whoever was already implementing it, so its new
 * methods arrive with a body. This one privileges the opposite: that adding syntax to the
 * language should **force** one to look at all the tools that walk it. Whoever does not want
 * that obligation extends {@code SimpleTreeVisitor}, which is where the JDK puts the default
 * bodies.
 *
 * <p>That is also why {@link #visitOther} receives a bare {@link Tree}: it is for nodes that are
 * none of the foreseen ones, not for those this visitor did not want to write.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along the walk
 */
public interface TreeVisitor<R, P> {

    R visitAnnotatedType(AnnotatedTypeTree node, P p);

    R visitAnnotation(AnnotationTree node, P p);

    R visitMethodInvocation(MethodInvocationTree node, P p);

    R visitAssert(AssertTree node, P p);

    R visitAssignment(AssignmentTree node, P p);

    R visitCompoundAssignment(CompoundAssignmentTree node, P p);

    R visitBinary(BinaryTree node, P p);

    R visitBlock(BlockTree node, P p);

    R visitBreak(BreakTree node, P p);

    R visitCase(CaseTree node, P p);

    R visitCatch(CatchTree node, P p);

    R visitClass(ClassTree node, P p);

    R visitConditionalExpression(ConditionalExpressionTree node, P p);

    R visitContinue(ContinueTree node, P p);

    R visitDoWhileLoop(DoWhileLoopTree node, P p);

    R visitErroneous(ErroneousTree node, P p);

    R visitExpressionStatement(ExpressionStatementTree node, P p);

    R visitEnhancedForLoop(EnhancedForLoopTree node, P p);

    R visitForLoop(ForLoopTree node, P p);

    R visitIdentifier(IdentifierTree node, P p);

    R visitIf(IfTree node, P p);

    R visitImport(ImportTree node, P p);

    R visitArrayAccess(ArrayAccessTree node, P p);

    R visitLabeledStatement(LabeledStatementTree node, P p);

    R visitLiteral(LiteralTree node, P p);

    R visitAnyPattern(AnyPatternTree node, P p);

    R visitBindingPattern(BindingPatternTree node, P p);

    R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p);

    R visitConstantCaseLabel(ConstantCaseLabelTree node, P p);

    R visitPatternCaseLabel(PatternCaseLabelTree node, P p);

    R visitDeconstructionPattern(DeconstructionPatternTree node, P p);

    R visitMethod(MethodTree node, P p);

    R visitModifiers(ModifiersTree node, P p);

    R visitNewArray(NewArrayTree node, P p);

    R visitNewClass(NewClassTree node, P p);

    R visitLambdaExpression(LambdaExpressionTree node, P p);

    R visitPackage(PackageTree node, P p);

    R visitParenthesized(ParenthesizedTree node, P p);

    R visitReturn(ReturnTree node, P p);

    R visitMemberSelect(MemberSelectTree node, P p);

    R visitMemberReference(MemberReferenceTree node, P p);

    R visitEmptyStatement(EmptyStatementTree node, P p);

    R visitSwitch(SwitchTree node, P p);

    R visitSwitchExpression(SwitchExpressionTree node, P p);

    R visitSynchronized(SynchronizedTree node, P p);

    R visitThrow(ThrowTree node, P p);

    R visitCompilationUnit(CompilationUnitTree node, P p);

    R visitTry(TryTree node, P p);

    R visitParameterizedType(ParameterizedTypeTree node, P p);

    R visitUnionType(UnionTypeTree node, P p);

    R visitIntersectionType(IntersectionTypeTree node, P p);

    R visitArrayType(ArrayTypeTree node, P p);

    R visitTypeCast(TypeCastTree node, P p);

    R visitPrimitiveType(PrimitiveTypeTree node, P p);

    R visitTypeParameter(TypeParameterTree node, P p);

    R visitInstanceOf(InstanceOfTree node, P p);

    R visitUnary(UnaryTree node, P p);

    R visitVariable(VariableTree node, P p);

    R visitWhileLoop(WhileLoopTree node, P p);

    R visitWildcard(WildcardTree node, P p);

    R visitModule(ModuleTree node, P p);

    R visitExports(ExportsTree node, P p);

    R visitOpens(OpensTree node, P p);

    R visitProvides(ProvidesTree node, P p);

    R visitRequires(RequiresTree node, P p);

    R visitUses(UsesTree node, P p);

    /** A node that is none of the foreseen ones: an implementation of one's own. */
    R visitOther(Tree node, P p);

    R visitYield(YieldTree node, P p);
}
