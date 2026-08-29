package java.lang.reflect;

/**
 * An annotated use of a parameterized type: {@code @A Map<@B String, @C Integer>}.
 *
 * <p>The distinction this interface carries is between an annotation on the type itself and one on
 * its arguments. {@code @A} above is an annotation of <em>this</em> node, reachable through the
 * inherited {@link AnnotatedElement} methods; {@code @B} and {@code @C} belong to the argument uses
 * and are reachable only through {@link #getAnnotatedActualTypeArguments}. Both survive into the
 * class file even though the type arguments themselves are erased — the {@code RuntimeVisible}
 * <em>TypeAnnotations</em> attribute records them positionally.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete, and matching the JDK method for method.
 */
public interface AnnotatedParameterizedType extends AnnotatedType {

    /**
     * Returns the annotated uses of this type's actual type arguments.
     *
     * @return the annotated type arguments, in declaration order
     */
    AnnotatedType[] getAnnotatedActualTypeArguments();

    /**
     * Returns the annotated use of the type this one is a member of, or {@code null}.
     *
     * @return the annotated owner type, or {@code null}
     */
    AnnotatedType getAnnotatedOwnerType();
}
