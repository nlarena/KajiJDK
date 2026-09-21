package com.sun.source.tree;

/**
 * Any node of the syntax tree of a Java source file.
 *
 * <h2>What this package is</h2>
 *
 * <p>The syntax tree as an external tool sees it: an IDE, a style analyser, an annotation
 * processor that wants to look at the code and not only at the signatures. It is the
 * *syntactic* view, sister of {@code javax.lang.model}, which is the *semantic* view -- the same
 * program told twice, once as it was written and once as it was resolved.
 *
 * <h2>The asymmetry that has to be understood: 117 kinds of node and 76 interfaces</h2>
 *
 * <p>{@link Kind} has more constants than there are interfaces in the package, and it is not
 * untidiness. All the binary operators share {@link BinaryTree} and are told apart by their
 * {@code Kind}: {@code PLUS}, {@code MULTIPLY}, {@code AND}. The same for the unary ones in
 * {@link UnaryTree} -- there live {@code PREFIX_INCREMENT} and {@code POSTFIX_INCREMENT}, which
 * have the same shape and different meanings -- and the five type declarations in
 * {@link ClassTree}.
 *
 * <p>The practical consequence: <strong>asking for the Java type is not always enough</strong>.
 * An {@code instanceof BinaryTree} does not say which operator it is.
 *
 * <h2>The two ways of walking it</h2>
 *
 * <p>{@link #getKind} for a loose decision; {@link TreeVisitor} in order to attend to them all,
 * with the compiler watching that none is missing.
 */
public interface Tree {

    /**
     * What kind of node this is.
     *
     * <p>Each constant knows which the interface that represents it is, and {@link #asInterface}
     * returns it. It is what allows one to go from the constant to the type without a table
     * written by hand -- and what makes it visible that several constants share an interface.
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

        /** An implementation of one's own that is none of the previous ones. */
        OTHER(null),

        YIELD(YieldTree.class);

        // Private and final: it is a datum of the constant, not a computation. `Class<? extends
        // Tree>` and
                // not `Class<?>` because the bound is true and saying so avoids a cast at every
                // use.
        private final Class<? extends Tree> associatedInterface;

        Kind(Class<? extends Tree> intf) {
            this.associatedInterface = intf;
        }

        /**
         * The interface that represents this kind of node, or {@code null} for {@link #OTHER}.
         *
         * <p>It is not injective: several constants return the same interface -- see {@link Tree}'s
         * note about why there are 117 constants and 76 interfaces.
         */
        public Class<? extends Tree> asInterface() {
            return this.associatedInterface;
        }
    }

    /** What kind of node it is. */
    Kind getKind();

    /**
     * It passes this node to the visitor.
     *
     * @param <R> what the visitor returns
     * @param <D> the datum that is carried along to it
     */
    <R, D> R accept(TreeVisitor<R, D> visitor, D data);
}
