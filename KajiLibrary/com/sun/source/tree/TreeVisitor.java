package com.sun.source.tree;

/**
 * Un visitante del arbol de sintaxis.
 *
 * <h2>Que aporta sobre un {@code switch}</h2>
 *
 * <p>Que el compilador cuente. Si el JDK agrega un tipo de nodo —y lo hace en cada version que
 * agrega sintaxis: los patrones, los records, los modulos— una implementacion de esta interfaz deja
 * de compilar hasta que alguien decida que hacer con el nodo nuevo. Un {@code switch} sobre
 * {@link Tree.Kind} se queda callado y cae en su rama por defecto.
 *
 * <h2>Todos abstractos, a diferencia de {@link com.sun.source.doctree.DocTreeVisitor}</h2>
 *
 * <p>Y la diferencia es una decision, no un descuido de un lado o del otro. El arbol de
 * documentacion privilegia no romper a quien ya lo implementaba, asi que sus metodos nuevos llegan
 * con cuerpo. Este privilegia lo contrario: que agregar sintaxis al lenguaje **obligue** a mirar
 * todas las herramientas que lo recorren. Quien no quiera esa obligacion extiende
 * {@code SimpleTreeVisitor}, que es donde el JDK pone los cuerpos por defecto.
 *
 * <p>Por eso tambien {@link #visitOther} recibe un {@link Tree} pelado: es para nodos que no son
 * ninguno de los previstos, no para los que este visitante no quiso escribir.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra por el recorrido
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

    /** Un nodo que no es ninguno de los previstos: una implementacion propia. */
    R visitOther(Tree node, P p);

    R visitYield(YieldTree node, P p);
}
