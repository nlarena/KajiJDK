package java.lang.reflect;

/**
 * A potentially annotated <em>use</em> of a type — not a type, a use of one.
 *
 * <p>The distinction is the whole reason this hierarchy exists in parallel to {@link Type}. Since
 * Java 8, annotations can be written where a type appears rather than only where a declaration
 * does: {@code @NonNull String}, {@code List<@Readonly Object>}, {@code int @Nullable []}. Two uses
 * of {@code String} in the same method can carry different annotations, so the annotations cannot
 * live on the {@code String} type object, which is shared. They live on the use, and
 * {@code AnnotatedType} is the reified use: a {@link Type} plus the annotations written at that
 * one syntactic position.
 *
 * <p>{@link #getType} is the escape hatch back to the erasure-era model. The five sub-interfaces
 * mirror the five shapes of {@link Type} one-for-one, so an {@code AnnotatedType} tree can be
 * walked in step with the {@code Type} tree underneath it.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>The method set is semantically identical to the JDK's. One structural difference: the JDK
 * redeclares {@code getAnnotation}, {@code getAnnotations} and {@code getDeclaredAnnotations} as
 * abstract even though {@link AnnotatedElement} already declares them — redundant to the compiler,
 * but it is where the javadoc gets to say "the annotations on this <em>use</em>", a genuinely
 * different contract from the inherited one. None of the three is repeated here, because our javac
 * mishandles both spellings and the repetition buys nothing at runtime:
 *
 * <ul>
 *   <li>The two {@code Annotation[]}-returning ones are rejected outright — re-declaring an inherited
 *       method whose return type is an array of a non-{@code java.lang} type read back from a class
 *       file fails the return-compatibility check, with the component type resolving to {@code ?}.</li>
 *   <li>{@code <T extends Annotation> T getAnnotation(Class<T>)} compiles, but emits a corrupt
 *       {@code Signature}: a type-parameter bound is written with the source simple name
 *       ({@code <T:LAnnotation;>}) unless the bound type also appears in an ordinary position in the
 *       same file, and this interface has no such position to offer.</li>
 * </ul>
 *
 * <p>Both are reported to the compiler session with repros. Neither omission is observable: the three
 * methods are still inherited, still abstract, still part of the interface.
 */
public interface AnnotatedType extends AnnotatedElement {

    /**
     * Returns the annotated use of the type that owns this one, or {@code null} if there is none.
     *
     * <p>For a nested type {@code Outer.@A Inner}, the owner is the use of {@code Outer}. Only
     * {@link ParameterizedType} and inner-class uses have owners.
     *
     * @return the annotated owner type, or {@code null}
     */
    default AnnotatedType getAnnotatedOwnerType() {
        // The JDK's default returns null and lets the five sub-interfaces override; a use of a
        // plain top-level type genuinely has no owner.
        return null;
    }

    /**
     * Returns the underlying type this is a use of.
     *
     * @return the type
     */
    Type getType();

    // Re-declared from AnnotatedElement to match the JDK's own surface (it repeats them here).

    <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass);

    java.lang.annotation.Annotation[] getAnnotations();

    java.lang.annotation.Annotation[] getDeclaredAnnotations();
}
