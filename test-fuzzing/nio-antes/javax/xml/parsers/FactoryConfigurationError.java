package javax.xml.parsers;

/**
 * KajiLibrary's javax.xml.parsers.FactoryConfigurationError -- there is no factory.
 *
 * <p>It is an {@link Error} and not an exception, which seems excessive and is not: it means the
 * XML implementation named in the configuration <b>does not exist</b> or could not be loaded. It is
 * not a request that failed, it is the platform put together wrong, and there is nothing a {@code
 * catch} around the call can do about it.
 *
 * <h2>The two ways to the cause</h2>
 *
 * <p>{@link #getException} and {@link #getCause} return the same. The first is from 2000, before
 * {@code Throwable} had a chained cause; the second arrived with Java 1.4 and is the one a {@code
 * printStackTrace} sees. Both are kept, and the second is implemented in terms of the first so that
 * they cannot disagree.
 *
 * <p>{@link #getMessage} also has a twist: if no message of its own was given, it returns the
 * cause's. Without that, the commonest error of this package --a misspelt class in a property--
 * would print without saying which.
 */
public class FactoryConfigurationError extends Error {

    private static final long serialVersionUID = -827108682472263355L;

    /** The cause; see the class note on why {@code Throwable}'s is not used. */
    private Exception exception;

    /** Without detail. */
    public FactoryConfigurationError() {
        super();
        this.exception = null;
    }

    /** With a message. */
    public FactoryConfigurationError(String msg) {
        super(msg);
        this.exception = null;
    }

    /** Wrapping what really failed. */
    public FactoryConfigurationError(Exception e) {
        super(e.toString());
        this.exception = e;
    }

    /** With both things. */
    public FactoryConfigurationError(Exception e, String msg) {
        super(msg);
        this.exception = e;
    }

    /** Its own message, or the cause's if there is none. See the class note. */
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && this.exception != null) {
            return this.exception.getMessage();
        }
        return message;
    }

    /** The cause, the old way. */
    public Exception getException() {
        return this.exception;
    }

    /** The cause, in the form {@code Throwable} understands. It is the same one. */
    public Throwable getCause() {
        return this.exception;
    }
}
