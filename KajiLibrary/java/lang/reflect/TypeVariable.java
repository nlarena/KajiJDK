package java.lang.reflect;

/**
 * A type variable: the {@code T} in {@code class Box<T>} or {@code <T> void m(T t)}.
 *
 * <p>A type variable is not a type in the runtime's sense — erasure replaces every {@code T} with
 * its leftmost bound, so no {@code Class} object survives for it. What survives is the
 * {@code Signature} attribute, and this interface is the reading of it.
 *
 * <p>The type parameter {@code D} is the interesting part of the declaration. It records
 * <em>where</em> the variable was declared, in the static type: read the parameters of a
 * {@code Method} and you get {@code TypeVariable<Method>[]}, so {@link #getGenericDeclaration}
 * returns a {@code Method} with no cast at the call site. That is why this interface and
 * {@link GenericDeclaration} refer to each other.
 *
 * <p>{@link #getBounds} always reports at least one bound: an unbounded {@code <T>} declares
 * {@code Object}, because erasure has to have something to erase to.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete. The JDK's javadoc marks the whole interface as throwing
 * {@link GenericSignatureFormatError} and {@link TypeNotPresentException} from its accessors —
 * both are unchecked, so they do not appear in the signatures, and both classes already exist here.
 */
public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {

    /**
     * Returns the upper bounds of this type variable. An unbounded variable reports {@code Object}.
     *
     * @return the bounds, in declaration order
     */
    Type[] getBounds();

    /**
     * Returns the class, method or constructor that declared this type variable.
     *
     * @return the generic declaration
     */
    D getGenericDeclaration();

    /**
     * Returns the name of this type variable as it appears in source.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the annotated uses of this variable's bounds.
     *
     * <p>Reports one element per bound, in the same order as {@link #getBounds}. An unbounded
     * variable reports a single, unannotated use of {@code Object}.
     *
     * @return the annotated bounds
     */
    AnnotatedType[] getAnnotatedBounds();
}
