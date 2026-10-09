package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.AnnotationFormatError — the `.class` carried syntactically
 * broken annotations.
 *
 * <p>It is an {@link Error} and not an exception on purpose: it does not describe a program that
 * asked for something impossible but a class file that is corrupt or was produced by a tool with a
 * bug. Nobody can reasonably recover from that, so it is not worth forcing anyone to catch it.
 *
 * <p>It should not be confused with {@link AnnotationTypeMismatchException}: that one shows up when
 * the bytes parsed fine but the value is not of the type the element declares --the annotation's
 * `.class` and the annotated type's were compiled separately and drifted apart--. This one shows up
 * earlier, when the bytes do not even form an annotation.
 *
 * <p>The class is complete: three constructors that only delegate. Whoever parses annotations in
 * this VM is in Rust and does not construct it today; that takes nothing away from the type,
 * because code writing or reading annotations by hand has to be able to name it and catch it.
 * <p>The UID is left to be computed instead of being declared. `ObjectStreamClass` works it out
 * per the specification --SHA-1 over the canonical form, static initializer included-- so a
 * computed one is right by construction, while a hand-written constant copied from nowhere would be
 * a number two JVMs could disagree on with nothing to notice it by. That is the worst way of being
 * wrong this API has; see {@code ObjectStreamClass}'s note.
 */
public class AnnotationFormatError extends Error {

    public AnnotationFormatError(String message) {
        super(message);
    }

    public AnnotationFormatError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The detail is left to the cause's {@link Throwable#toString()}, not to a text of our own.
     *
     * <p>It is the canonical shape of "wrapping": a format error nearly always comes out of an
     * {@code IOException} or a parser that ran out of bytes, and that exception already knows how to
     * explain itself better than any fixed phrase we could put here.
     */
    public AnnotationFormatError(Throwable cause) {
        super(cause);
    }
}
