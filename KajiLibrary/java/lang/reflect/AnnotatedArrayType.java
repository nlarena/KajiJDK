package java.lang.reflect;

/**
 * An annotated use of an array type: {@code String @NonNull []}, {@code int[] @A []}.
 *
 * <p>Array type annotations read right-to-left, which is the one genuinely surprising corner of
 * JSR 308 syntax. In {@code String @A [] @B []} the {@code @A} annotates the outer array type and
 * {@code @B} the inner one — so this node's own annotations (inherited from {@link AnnotatedElement})
 * describe the array, and {@link #getAnnotatedGenericComponentType} walks one level in.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete. {@code getAnnotatedOwnerType} is re-declared abstract exactly as the JDK does: an
 * array type never has an owner, so the sub-interface removes the inherited default rather than
 * letting callers get a silent {@code null} that means "not applicable" instead of "no owner".
 */
public interface AnnotatedArrayType extends AnnotatedType {

    /**
     * Returns the annotated use of this array type's component type.
     *
     * @return the annotated component type
     */
    AnnotatedType getAnnotatedGenericComponentType();

    /**
     * Returns the annotated use of the owning type. Always {@code null} for an array type.
     *
     * @return {@code null}
     */
    AnnotatedType getAnnotatedOwnerType();
}
