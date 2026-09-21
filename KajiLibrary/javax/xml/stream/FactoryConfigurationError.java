package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.FactoryConfigurationError -- when there is no StAX implementation
 * to give the caller.
 *
 * <p>It is an {@link Error} and not an exception, and the choice makes sense: that no parser is
 * installed is not a problem of the document nor of the arguments, it is an incomplete deployment.
 * There is nothing the caller can do at run time to fix it, so forcing them to write a {@code
 * catch} would only add noise. It is the same decision as {@link
 * javax.xml.transform.TransformerFactoryConfigurationError} for XSLT.
 *
 * <p>In this library it is reached when a configured class cannot be used. (The note said it was
 * the normal path, because there was no StAX parser here; the package now has its own
 * implementation and the factories return it.)
 *
 * <h2>The three overridden methods</h2>
 *
 * <p>{@link #getException} and {@link #getCause} return the same. The first is from 2004 and the
 * second appeared with {@code Throwable}'s chained causes; both are kept because there is code that
 * calls each.
 *
 * <p>{@link #getMessage} has a cascade worth explaining: if there is a message of its own it
 * returns it; if not, the wrapped exception's; and if the wrapped one has none either, the name of
 * its class. Without that last branch, wrapping an exception without a message --which is normal
 * for a {@code ClassNotFoundException} on some VMs-- would give an error with a null message, which
 * is the trace that says nothing.
 */
public class FactoryConfigurationError extends Error {

    /**
     * The exception that caused this, if there was one.
     *
     * <p>Without a modifier, as in the original: it is not part of the public API, but {@code
     * getCause} and {@code getMessage} read it.
     */
    Exception nested;

    /** Without message nor cause. */
    public FactoryConfigurationError() {
        super();
    }

    /**
     * Wrapping the exception that prevented building the factory.
     *
     * @param e the inner exception
     */
    public FactoryConfigurationError(Exception e) {
        nested = e;
    }

    /**
     * With cause and message, in that order.
     *
     * <p>That both orders exist --this one and {@link #FactoryConfigurationError(String,
     * Exception)}-- is history, not design: both remained for compatibility and do exactly the
     * same.
     *
     * @param e the inner exception
     * @param msg the message
     */
    public FactoryConfigurationError(Exception e, String msg) {
        super(msg);
        nested = e;
    }

    /**
     * With message and cause, in that order.
     *
     * @param msg the message
     * @param e the inner exception
     */
    public FactoryConfigurationError(String msg, Exception e) {
        super(msg);
        nested = e;
    }

    /**
     * With a message only.
     *
     * @param msg the message
     */
    public FactoryConfigurationError(String msg) {
        super(msg);
    }

    /**
     * The wrapped exception, or null.
     *
     * @return the inner one
     */
    public Exception getException() {
        return nested;
    }

    /**
     * The same as {@link #getException}, with the name {@code Throwable} uses.
     *
     * @return the inner one
     */
    public Throwable getCause() {
        return nested;
    }

    /**
     * Its own message; if there is none, the wrapped one's; if not that either, the name of its
     * class.
     *
     * @return the message, which can be null only if there is neither message nor cause
     */
    public String getMessage() {
        String msg = super.getMessage();
        if (msg != null) {
            return msg;
        }
        if (nested != null) {
            msg = nested.getMessage();
            if (msg == null) {
                msg = nested.getClass().toString();
            }
        }
        return msg;
    }
}
