package java.lang.annotation;

import java.lang.reflect.Method;

/**
 * KajiLibrary's java.lang.annotation.AnnotationTypeMismatchException — an annotation's element holds
 * a value of a type other than the one it declares.
 *
 * <p>It is the symptom of a half-finished recompilation. Someone wrote {@code int value();}, a third
 * party compiled {@code @Config(3)}, then the author changed the element to {@code String value();}
 * and recompiled <strong>only their own</strong> annotation. The user's `.class` still has an
 * integer inside; the annotation's already promises a text. Nobody is lying at the moment they were
 * compiled: they drifted apart. The clash is only discovered when reflection brings them together,
 * and that is why it lives at run time and the compiler cannot detect it.
 *
 * <p>It differs from {@link AnnotationFormatError} in that here the bytes are well formed; what
 * fails is the agreement between two files. And from {@link IncompleteAnnotationException} in that
 * there the element is simply <strong>not present</strong>, whereas here it is present with the
 * wrong type.
 *
 * <p>The class is complete. What normally constructs it is the annotation proxy when reading an
 * element, which this VM does not do yet; the type is needed all the same, because any code calling
 * {@code getAnnotation(...).value()} has to be able to catch it.
 * <p>The UID is left to be computed instead of being declared. `ObjectStreamClass` works it out
 * per the specification --SHA-1 over the canonical form, static initializer included-- so a
 * computed one is right by construction, while a hand-written constant copied from nowhere would be
 * a number two JVMs could disagree on with nothing to notice it by. That is the worst way of being
 * wrong this API has; see {@code ObjectStreamClass}'s note.
 */
public class AnnotationTypeMismatchException extends RuntimeException {

    /**
     * `transient` because a {@link Method} does not travel: on deserialising the exception the field
     * comes back `null`, which is why {@link #element()} documents that it may not be available.
     * {@link #foundType} does travel, and it is the only thing that survives from one side to the
     * other.
     */
    private final transient Method element;

    private final String foundType;

    /**
     * The message is built here, in the `super`, and not in an overridden `getMessage()`.
     *
     * <p>It has to come before the fields are assigned --that is the language's rule-- and so the
     * text is built out of the parameters. Both accept `null` on purpose: whoever detects the clash
     * may not have the {@link Method} to hand, and the concatenation turns them into the literal
     * "null" without blowing up. Trading that for a check that throws
     * {@code NullPointerException} would turn a poor diagnostic into a failure, which is worse.
     */
    public AnnotationTypeMismatchException(Method element, String foundType) {
        super("Incorrectly typed data found for annotation element " + element
                + " (Found data of type " + foundType + ")");
        this.element = element;
        this.foundType = foundType;
    }

    public Method element() {
        return this.element;
    }

    public String foundType() {
        return this.foundType;
    }
}
