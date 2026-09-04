package com.sun.source.tree;

/**
 * Cualquier nodo del arbol de sintaxis de un archivo fuente de Java.
 *
 * <h2>Que es este paquete</h2>
 *
 * <p>El arbol de sintaxis tal como lo ve una herramienta externa: un IDE, un analizador de estilo,
 * un procesador de anotaciones que quiere mirar el codigo y no solo las firmas. Es la vista
 * *sintactica*, hermana de {@code javax.lang.model}, que es la vista *semantica* — el mismo programa
 * contado dos veces, una como se escribio y otra como quedo resuelto.
 *
 * <h2>La asimetria que hay que entender: 117 clases de nodo y 76 interfaces</h2>
 *
 * <p>{@link Kind} tiene mas constantes que interfaces hay en el paquete, y no es desprolijidad.
 * Todos los operadores binarios comparten {@link BinaryTree} y se distinguen por su {@code Kind}:
 * {@code PLUS}, {@code MULTIPLY}, {@code AND}. Lo mismo los unarios en {@link UnaryTree} —ahi viven
 * {@code PREFIX_INCREMENT} y {@code POSTFIX_INCREMENT}, que tienen la misma forma y significados
 * distintos— y las cinco declaraciones de tipo en {@link ClassTree}.
 *
 * <p>La consecuencia practica: <strong>preguntar por el tipo Java no siempre alcanza</strong>. Un
 * {@code instanceof BinaryTree} no dice que operador es.
 *
 * <h2>Las dos formas de recorrerlo</h2>
 *
 * <p>{@link #getKind} para una decision suelta; {@link TreeVisitor} para atender a todos, con el
 * compilador vigilando que no falte ninguno.
 */
public interface Tree {

    /**
     * Que clase de nodo es este.
     *
     * <p>Cada constante sabe cual es la interfaz que la representa, y {@link #asInterface} la
     * devuelve. Es lo que permite ir de la constante al tipo sin una tabla escrita a mano — y lo
     * que hace visible que varias constantes compartan interfaz.
     */
    enum Kind {

        ANNOTATED_TYPE(AnnotatedTypeTree.class),

        ANNOTATION(AnnotationTree.class),

        TYPE_ANNOTATION(AnnotationTree.class),

        ARRAY_ACCESS(ArrayAccessTree.class),

        ARRAY_TYPE(ArrayTypeTree.class),

        ASSERT(AssertTree.class),

        ASSIGNMENT(AssignmentTree.class),

        BLOCK(BlockTree.class),

        BREAK(BreakTree.class),

        CASE(CaseTree.class),

        CATCH(CatchTree.class),

        CLASS(ClassTree.class),

        COMPILATION_UNIT(CompilationUnitTree.class),

        CONDITIONAL_EXPRESSION(ConditionalExpressionTree.class),

        CONTINUE(ContinueTree.class),

        DO_WHILE_LOOP(DoWhileLoopTree.class),

        ENHANCED_FOR_LOOP(EnhancedForLoopTree.class),

        EXPRESSION_STATEMENT(ExpressionStatementTree.class),

        MEMBER_SELECT(MemberSelectTree.class),

        MEMBER_REFERENCE(MemberReferenceTree.class),

        FOR_LOOP(ForLoopTree.class),

        IDENTIFIER(IdentifierTree.class),

        IF(IfTree.class),

        IMPORT(ImportTree.class),

        INSTANCE_OF(InstanceOfTree.class),

        LABELED_STATEMENT(LabeledStatementTree.class),

        METHOD(MethodTree.class),

        METHOD_INVOCATION(MethodInvocationTree.class),

        MODIFIERS(ModifiersTree.class),

        NEW_ARRAY(NewArrayTree.class),

        NEW_CLASS(NewClassTree.class),

        LAMBDA_EXPRESSION(LambdaExpressionTree.class),

        PACKAGE(PackageTree.class),

        PARENTHESIZED(ParenthesizedTree.class),

        ANY_PATTERN(AnyPatternTree.class),

        BINDING_PATTERN(BindingPatternTree.class),

        DEFAULT_CASE_LABEL(DefaultCaseLabelTree.class),

        CONSTANT_CASE_LABEL(ConstantCaseLabelTree.class),

        PATTERN_CASE_LABEL(PatternCaseLabelTree.class),

        DECONSTRUCTION_PATTERN(DeconstructionPatternTree.class),

        PRIMITIVE_TYPE(PrimitiveTypeTree.class),

        RETURN(ReturnTree.class),

        EMPTY_STATEMENT(EmptyStatementTree.class),

        SWITCH(SwitchTree.class),

        SWITCH_EXPRESSION(SwitchExpressionTree.class),

        SYNCHRONIZED(SynchronizedTree.class),

        THROW(ThrowTree.class),

        TRY(TryTree.class),

        PARAMETERIZED_TYPE(ParameterizedTypeTree.class),

        UNION_TYPE(UnionTypeTree.class),

        INTERSECTION_TYPE(IntersectionTypeTree.class),

        TYPE_CAST(TypeCastTree.class),

        TYPE_PARAMETER(TypeParameterTree.class),

        VARIABLE(VariableTree.class),

        WHILE_LOOP(WhileLoopTree.class),

        POSTFIX_INCREMENT(UnaryTree.class),

        POSTFIX_DECREMENT(UnaryTree.class),

        PREFIX_INCREMENT(UnaryTree.class),

        PREFIX_DECREMENT(UnaryTree.class),

        UNARY_PLUS(UnaryTree.class),

        UNARY_MINUS(UnaryTree.class),

        BITWISE_COMPLEMENT(UnaryTree.class),

        LOGICAL_COMPLEMENT(UnaryTree.class),

        MULTIPLY(BinaryTree.class),

        DIVIDE(BinaryTree.class),

        REMAINDER(BinaryTree.class),

        PLUS(BinaryTree.class),

        MINUS(BinaryTree.class),

        LEFT_SHIFT(BinaryTree.class),

        RIGHT_SHIFT(BinaryTree.class),

        UNSIGNED_RIGHT_SHIFT(BinaryTree.class),

        LESS_THAN(BinaryTree.class),

        GREATER_THAN(BinaryTree.class),

        LESS_THAN_EQUAL(BinaryTree.class),

        GREATER_THAN_EQUAL(BinaryTree.class),

        EQUAL_TO(BinaryTree.class),

        NOT_EQUAL_TO(BinaryTree.class),

        AND(BinaryTree.class),

        XOR(BinaryTree.class),

        OR(BinaryTree.class),

        CONDITIONAL_AND(BinaryTree.class),

        CONDITIONAL_OR(BinaryTree.class),

        MULTIPLY_ASSIGNMENT(CompoundAssignmentTree.class),

        DIVIDE_ASSIGNMENT(CompoundAssignmentTree.class),

        REMAINDER_ASSIGNMENT(CompoundAssignmentTree.class),

        PLUS_ASSIGNMENT(CompoundAssignmentTree.class),

        MINUS_ASSIGNMENT(CompoundAssignmentTree.class),

        LEFT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        AND_ASSIGNMENT(CompoundAssignmentTree.class),

        XOR_ASSIGNMENT(CompoundAssignmentTree.class),

        OR_ASSIGNMENT(CompoundAssignmentTree.class),

        INT_LITERAL(LiteralTree.class),

        LONG_LITERAL(LiteralTree.class),

        FLOAT_LITERAL(LiteralTree.class),

        DOUBLE_LITERAL(LiteralTree.class),

        BOOLEAN_LITERAL(LiteralTree.class),

        CHAR_LITERAL(LiteralTree.class),

        STRING_LITERAL(LiteralTree.class),

        NULL_LITERAL(LiteralTree.class),

        UNBOUNDED_WILDCARD(WildcardTree.class),

        EXTENDS_WILDCARD(WildcardTree.class),

        SUPER_WILDCARD(WildcardTree.class),

        ERRONEOUS(ErroneousTree.class),

        INTERFACE(ClassTree.class),

        ENUM(ClassTree.class),

        ANNOTATION_TYPE(ClassTree.class),

        MODULE(ModuleTree.class),

        EXPORTS(ExportsTree.class),

        OPENS(OpensTree.class),

        PROVIDES(ProvidesTree.class),

        RECORD(ClassTree.class),

        REQUIRES(RequiresTree.class),

        USES(UsesTree.class),

        /** Una implementacion propia que no es ninguna de las anteriores. */
        OTHER(null),

        YIELD(YieldTree.class);

        // Privado y final: es un dato de la constante, no un calculo. `Class<? extends Tree>` y no
        // `Class<?>` porque el limite es cierto y decirlo evita un cast en todo uso.
        private final Class<? extends Tree> interfazAsociada;

        Kind(Class<? extends Tree> intf) {
            this.interfazAsociada = intf;
        }

        /**
         * La interfaz que representa a esta clase de nodo, o {@code null} para {@link #OTHER}.
         *
         * <p>No es inyectiva: varias constantes devuelven la misma interfaz — ver la nota de
         * {@link Tree} sobre por que hay 117 constantes y 76 interfaces.
         */
        public Class<? extends Tree> asInterface() {
            return this.interfazAsociada;
        }
    }

    /** Que clase de nodo es. */
    Kind getKind();

    /**
     * Le pasa este nodo al visitante.
     *
     * @param <R> lo que devuelve el visitante
     * @param <D> el dato que se le arrastra
     */
    <R, D> R accept(TreeVisitor<R, D> visitor, D data);
}
