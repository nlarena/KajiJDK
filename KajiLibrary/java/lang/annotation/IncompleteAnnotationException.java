package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.IncompleteAnnotationException — the annotation is missing an
 * element the type declares.
 *
 * <p>Another drift between two compilations, sibling to {@link AnnotationTypeMismatchException}:
 * somebody added an element <strong>with no default value</strong> to an annotation type and
 * recompiled only that type. The `.class` files already using the annotation do not carry that
 * element, because it did not exist when they were compiled. Asking for it by reflection finds
 * nothing to return and no default to fall back on, and this is what comes out.
 *
 * <p>The detail that explains the whole design of annotations: an element with a {@code default}
 * never causes this exception. That is why adding elements with a default value is backwards
 * compatible and adding them without one is not.
 *
 * <p>The class is complete. Both fields are mutable (not `final`) just as in the JDK; it was left
 * that way out of fidelity, not because anybody reassigns them.
 * <p>The UID is left to be computed instead of being declared. `ObjectStreamClass` works it out
 * per the specification --SHA-1 over the canonical form, static initializer included-- so a
 * computed one is right by construction, while a hand-written constant copied from nowhere would be
 * a number two JVMs could disagree on with nothing to notice it by. That is the worst way of being
 * wrong this API has; see {@code ObjectStreamClass}'s note.
 */
public class IncompleteAnnotationException extends RuntimeException {

    private Class<? extends Annotation> annotationType;

    private String elementName;

    /**
     * Both arguments are dereferenced before being assigned --{@code getName()} on one,
     * {@code toString()} on the other-- so a `null` comes out as a {@code NullPointerException}
     * from the `super`, which is exactly what the JDK documents.
     *
     * <p>The {@code elementName.toString()} looks redundant on a {@link String} and is not: it is
     * what forces the early failure. Without it, the concatenation would write the literal "null"
     * and the exception would be built quite happily with an invalid element name inside.
     */
    public IncompleteAnnotationException(Class<? extends Annotation> annotationType,
            String elementName) {
        super(annotationType.getName() + " missing element " + elementName.toString());

        this.annotationType = annotationType;
        this.elementName = elementName;
    }

    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    public String elementName() {
        return elementName;
    }
}
