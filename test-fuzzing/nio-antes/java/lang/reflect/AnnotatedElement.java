package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * Something in a program that can carry annotations: a class, a method, a field, a parameter, a
 * module, a use of a type.
 *
 * <p>The interface is the reason {@code @Deprecated} on a class and {@code @Deprecated} on a
 * parameter can be read by the same code. Reflection has no single base type for program elements —
 * {@link Class} and {@link Member} are unrelated — so annotation access is factored out here and
 * mixed into each of them separately.
 *
 * <h2>Present, directly present, associated</h2>
 *
 * <p>The six methods look redundant until the three-way distinction behind them is spelled out,
 * because it is what the whole design turns on:
 *
 * <ul>
 *   <li><em>directly present</em> — literally written on this element. The {@code getDeclared*}
 *       methods report exactly this.</li>
 *   <li><em>present</em> — directly present, or inherited from a superclass because the annotation
 *       type is itself annotated {@code @Inherited}. {@link #getAnnotation} reports this, which is
 *       why it can return something that appears nowhere in the element's own source.</li>
 *   <li><em>associated</em> — present, or contained in a present container annotation because the
 *       annotation type is {@code @Repeatable}. The {@code *ByType} methods report this, and it is
 *       the only view under which {@code @Schedule @Schedule} reads back as two annotations rather
 *       than as one synthetic {@code @Schedules} nobody wrote.</li>
 * </ul>
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Faithful to the JDK's method set. Two deliberate divergences, both in the defaults:
 *
 * <ul>
 *   <li>{@link #getAnnotationsByType} and {@link #getDeclaredAnnotationsByType} do <strong>not</strong>
 *       unwrap {@code @Repeatable} containers. The JDK routes this through the internal
 *       {@code sun.reflect.annotation.AnnotationSupport}, which reads the {@code @Repeatable} meta
 *       annotation off the queried type to learn its container type and then reflectively calls the
 *       container's {@code value()}. That needs {@code Method.invoke} on annotation proxies —
 *       which needs {@link Proxy}, which KajiJDK does not have. So a repeated annotation reads back
 *       here as its container, exactly as it is stored in the class file. Everything else about
 *       these methods is correct.</li>
 *   <li>The JDK's {@code getDeclaredAnnotationsByType} builds its result with a stream and a merge
 *       function (the {@code lambda$getDeclaredAnnotationsByType$0} you can see in its class file);
 *       this one uses a counting loop, because {@code Collection.stream} does not exist in
 *       KajiLibrary.</li>
 * </ul>
 */
public interface AnnotatedElement {

    /**
     * Returns whether an annotation of the given type is <em>present</em> on this element.
     *
     * @param annotationClass the annotation type to look for
     * @return {@code true} if an annotation of that type is present
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * Returns this element's annotation of the given type if one is <em>present</em>.
     *
     * @param <T> the annotation type
     * @param annotationClass the annotation type to look for
     * @return the annotation, or {@code null}
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns every annotation <em>present</em> on this element.
     *
     * @return the annotations; empty if there are none
     */
    Annotation[] getAnnotations();

    /**
     * Returns every annotation of the given type <em>associated with</em> this element.
     *
     * @param <T> the annotation type
     * @param annotationClass the annotation type to look for
     * @return the annotations; empty if there are none
     */
    default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return AnnotatedElement.<T>filterByType(getAnnotations(), annotationClass);
    }

    /**
     * Returns this element's annotation of the given type if one is <em>directly present</em>.
     *
     * @param <T> the annotation type
     * @param annotationClass the annotation type to look for
     * @return the annotation, or {@code null}
     */
    default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        Annotation[] declared = getDeclaredAnnotations();
        for (int i = 0; i < declared.length; i = i + 1) {
            if (annotationClass.equals(declared[i].annotationType())) {
                // The JDK spells this `annotationClass.cast(annotation)`; Class.cast does not exist
                // in KajiLibrary, and the annotationType() check above is the same test cast would
                // make, so the unchecked cast is sound here.
                return (T) declared[i];
            }
        }
        return null;
    }

    /**
     * Returns every annotation of the given type <em>directly present</em> on this element.
     *
     * @param <T> the annotation type
     * @param annotationClass the annotation type to look for
     * @return the annotations; empty if there are none
     */
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return AnnotatedElement.<T>filterByType(getDeclaredAnnotations(), annotationClass);
    }

    /**
     * Returns every annotation <em>directly present</em> on this element.
     *
     * @return the annotations; empty if there are none
     */
    Annotation[] getDeclaredAnnotations();

    // Shared back end of the two *ByType methods. A private static interface method, which is what
    // the JDK uses for the same purpose (its version is the synthetic lambda body). Two passes so
    // the result array can be allocated at exactly the right length -- reflection returns arrays the
    // caller may keep, so trailing nulls would be visible.
    private static <T extends Annotation> T[] filterByType(Annotation[] all, Class<T> annotationClass) {
        int matches = 0;
        for (int i = 0; i < all.length; i = i + 1) {
            if (annotationClass.equals(all[i].annotationType())) {
                matches = matches + 1;
            }
        }
        // Array.newInstance is the only way to build a T[] whose runtime component type is the
        // queried annotation type; `new T[n]` is not expressible. The cast is unchecked for the
        // usual erasure reason and correct because the component type came from annotationClass.
        T[] result = (T[]) Array.newInstance(annotationClass, matches);
        int next = 0;
        for (int i = 0; i < all.length; i = i + 1) {
            if (annotationClass.equals(all[i].annotationType())) {
                result[next] = (T) all[i];
                next = next + 1;
            }
        }
        return result;
    }
}
