package java.lang.reflect;

/**
 * An annotated use of a wildcard: {@code List<@A ? extends @B Number>}.
 *
 * <p>Two annotation positions, and they are different things: {@code @A} annotates the wildcard
 * itself and is reached through the inherited {@link AnnotatedElement} methods, while {@code @B}
 * annotates the bound and is reached through {@link #getAnnotatedUpperBounds}. As in
 * {@link WildcardType}, the bounds come back as arrays even though the language permits at most one
 * on each side, and an unbounded {@code ?} still reports an upper bound — the annotated use of
 * {@code Object}.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete, and matching the JDK method for method.
 */
public interface AnnotatedWildcardType extends AnnotatedType {

    /**
     * Returns the annotated uses of this wildcard's lower bounds, or an empty array if it has none.
     *
     * @return the annotated lower bounds
     */
    AnnotatedType[] getAnnotatedLowerBounds();

    /**
     * Returns the annotated uses of this wildcard's upper bounds.
     *
     * @return the annotated upper bounds
     */
    AnnotatedType[] getAnnotatedUpperBounds();

    /**
     * Returns the annotated use of the owning type. Always {@code null} for a wildcard.
     *
     * @return {@code null}
     */
    AnnotatedType getAnnotatedOwnerType();
}
