package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.TransformerFactoryConfigurationError -- there is no factory.
 *
 * <p>It is an {@link Error} and not an exception, and the choice has grounds even if it seems
 * harsh: it is thrown by {@link TransformerFactory#newInstance()}, which **declares nothing
 * checked**, because in the normal case --there is an XSLT implementation on the classpath--
 * failure is impossible. When it happens, it happens because of a badly put together deployment:
 * the jar is missing, or the system property names a class that does not exist. It is not a
 * condition the application's code can handle, it is a broken installation, and forcing a
 * `try`/`catch` on every call would be pure noise.
 *
 * <p>**Here it is thrown whenever no factory is configured**, and not because of a fault: this
 * library brings no XSLT processor. See the header of {@link TransformerFactory} for the criterion
 * -- the summary is that a `Transformer` that does not transform would be much worse than this
 * error.
 *
 * <p>Like {@link TransformerException}, it carries its own cause field that predates the platform's
 * chained causes, and with two differences from that one worth keeping in mind because they are not
 * the ones one would assume:
 *
 * <ul>
 *   <li>the field is {@code Exception} and not {@code Throwable}, so an underlying {@code Error}
 *       cannot be kept here;
 *   <li>{@link #getMessage} **falls back to the cause's message** when its own is null. It is the
 *       only class of the package that does so, and it is why the {@code (Exception, String)}
 *       constructor has its arguments in that odd order: the one added later ended up last.
 * </ul>
 */
public class TransformerFactoryConfigurationError extends Error {

    private static final long serialVersionUID = -6323715983680123667L;

    /** The cause, by the old name. See the header note. */
    private Exception exception;

    /** Without message nor cause. */
    public TransformerFactoryConfigurationError() {
        super();
        this.exception = null;
    }

    /**
     * With a message.
     *
     * @param msg the description of the error
     */
    public TransformerFactoryConfigurationError(String msg) {
        super(msg);
        this.exception = null;
    }

    /**
     * Wrapping an exception; the message comes from it.
     *
     * @param e the cause
     */
    public TransformerFactoryConfigurationError(Exception e) {
        super(e.toString());
        this.exception = e;
    }

    /**
     * With cause and message, in that order.
     *
     * @param e the cause
     * @param msg the description of the error
     */
    public TransformerFactoryConfigurationError(Exception e, String msg) {
        super(msg);
        this.exception = e;
    }

    /**
     * Its own message; if there is none, the cause's.
     *
     * <p>The cascade exists so that {@code new TransformerFactoryConfigurationError(e, null)} does
     * not lose the only thing that was known about the problem.
     */
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && exception != null) {
            return exception.getMessage();
        }
        return message;
    }

    /** The cause, by the old name. */
    public Exception getException() {
        return exception;
    }

    /** The cause, by the platform's name. */
    public Throwable getCause() {
        return exception;
    }
}
