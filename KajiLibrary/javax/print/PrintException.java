package javax.print;

/**
 * KajiLibrary's javax.print.PrintException -- something went wrong when printing.
 *
 * <p>The base of the print system's errors. The package's three interfaces that end in {@code
 * Exception} --{@link AttributeException}, {@link FlavorException} and {@link URIException}-- do
 * <b>not</b> inherit from this one: they are interfaces a subclass of this one implements to
 * explain why it failed.
 *
 * <p>It is an odd design and it has a reason: the cause of the failure may be more than one at a
 * time --an unsupported attribute <i>and</i> an unsupported format-- and single inheritance could
 * not say that. So whoever catches this asks with {@code instanceof} about each interface.
 *
 * <p>The constructors take {@link Exception} and not {@link Throwable}; it is from 2001 and stayed
 * that way.
 */
public class PrintException extends Exception {

    private static final long serialVersionUID = -5932531546705242471L;

    /** Without detail. */
    public PrintException() {
        super();
    }

    /** With a message. */
    public PrintException(String s) {
        super(s);
    }

    /** Wrapping another. */
    public PrintException(Exception e) {
        super(e);
    }

    /** With a message, wrapping another. */
    public PrintException(String s, Exception e) {
        super(s, e);
    }
}
