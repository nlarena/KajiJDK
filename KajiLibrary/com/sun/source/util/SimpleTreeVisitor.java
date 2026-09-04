package com.sun.source.util;

import com.sun.source.tree.*;

/**
 * Un visitante que manda todo a un solo lugar.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Para atender <strong>unos pocos</strong> tipos de nodo sin escribir los 68 metodos.
 * Sobrescribiendo solo los que interesan, el resto cae en {@link #defaultAction}.
 *
 * <p>Es lo contrario de implementar {@link TreeVisitor} directamente, que obliga a escribirlos todos
 * — y esa obligacion tambien tiene su valor: es lo que hace que agregar sintaxis al lenguaje rompa
 * la compilacion de las herramientas en vez de que la ignoren en silencio. Esta clase renuncia a eso
 * a cambio de brevedad.
 *
 * <p><strong>No recorre.</strong> Para eso esta {@link TreeScanner}.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra
 */
public class SimpleTreeVisitor<R, P> implements TreeVisitor<R, P> {

    /** Lo que devuelve {@link #defaultAction} si no se lo sobrescribe. */
    protected final R DEFAULT_VALUE;

    /** Con {@code null} como valor por omision. */
    protected SimpleTreeVisitor() {
        this.DEFAULT_VALUE = null;
    }

    /** Con ese valor por omision. */
    protected SimpleTreeVisitor(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** Lo que se hace con un nodo que no se sobrescribio. */
    protected R defaultAction(Tree node, P p) {
        return this.DEFAULT_VALUE;
    }

    /** Visita un nodo. {@code final}: el punto de extension es {@link #defaultAction}. */
    public final R visit(Tree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** Visita todos, y devuelve lo del ultimo. */
    public final R visit(Iterable<? extends Tree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            for (Tree node : nodes) {
                r = visit(node, p);
            }
        }
        return r;
    }


    public R visitAnnotatedType(AnnotatedTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitAnnotation(AnnotationTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitAssert(AssertTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitAssignment(AssignmentTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitBinary(BinaryTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitBlock(BlockTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitBreak(BreakTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitCase(CaseTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitCatch(CatchTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitClass(ClassTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitConditionalExpression(ConditionalExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitContinue(ContinueTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDoWhileLoop(DoWhileLoopTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitErroneous(ErroneousTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitExpressionStatement(ExpressionStatementTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitForLoop(ForLoopTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitIdentifier(IdentifierTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitIf(IfTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitImport(ImportTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitArrayAccess(ArrayAccessTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitLabeledStatement(LabeledStatementTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitLiteral(LiteralTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitAnyPattern(AnyPatternTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitBindingPattern(BindingPatternTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitConstantCaseLabel(ConstantCaseLabelTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitPatternCaseLabel(PatternCaseLabelTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDeconstructionPattern(DeconstructionPatternTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitMethod(MethodTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitModifiers(ModifiersTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitNewArray(NewArrayTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitNewClass(NewClassTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitLambdaExpression(LambdaExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitPackage(PackageTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitParenthesized(ParenthesizedTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitReturn(ReturnTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitMemberSelect(MemberSelectTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitMemberReference(MemberReferenceTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitEmptyStatement(EmptyStatementTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSwitch(SwitchTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSwitchExpression(SwitchExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSynchronized(SynchronizedTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitThrow(ThrowTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitCompilationUnit(CompilationUnitTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitTry(TryTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitParameterizedType(ParameterizedTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUnionType(UnionTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitIntersectionType(IntersectionTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitArrayType(ArrayTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitTypeCast(TypeCastTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitPrimitiveType(PrimitiveTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitTypeParameter(TypeParameterTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitInstanceOf(InstanceOfTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUnary(UnaryTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitVariable(VariableTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitWhileLoop(WhileLoopTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitWildcard(WildcardTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitModule(ModuleTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitExports(ExportsTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitOpens(OpensTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitProvides(ProvidesTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitRequires(RequiresTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUses(UsesTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitOther(Tree node, P p) {
        return defaultAction(node, p);
    }

    public R visitYield(YieldTree node, P p) {
        return defaultAction(node, p);
    }
}
