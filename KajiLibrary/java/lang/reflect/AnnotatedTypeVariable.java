package java.lang.reflect;

/**
 * An annotated use of a type variable: the {@code @A T} in {@code void m(@A T t)}.
 *
 * <p>{@link #getAnnotatedBounds} is the reason this is more than a marker. A type variable's bounds
 * are written once, at the declaration ({@code <T extends @NonNull Number>}), while this node is a
 * <em>use</em> somewhere else entirely — so the bounds it reports are borrowed from the declaration,
 * not from the use site. That is the one place in this hierarchy where a use node exposes something
 * that is not local to the use.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete, and matching the JDK method for method.
 */
public interface AnnotatedTypeVariable extends AnnotatedType {

    /**
     * Returns the annotated uses of this type variable's bounds, taken from its declaration.
     *
     * @return the annotated bounds
     */
    AnnotatedType[] getAnnotatedBounds();

    /**
     * Returns the annotated use of the owning type. Always {@code null} for a type variable.
     *
     * @return {@code null}
     */
    AnnotatedType getAnnotatedOwnerType();
}
