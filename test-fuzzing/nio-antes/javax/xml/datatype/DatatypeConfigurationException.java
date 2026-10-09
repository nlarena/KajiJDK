package javax.xml.datatype;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeConfigurationException -- a {@link DatatypeFactory}
 * could not be obtained.
 *
 * <p>It is the exception of the search for an implementation, not of the data: a badly written
 * lexical value raises {@link IllegalArgumentException}, not this. This means there is no
 * implementation available, or that the one named could not be loaded.
 *
 * <p>It is <b>checked</b>, and on purpose: it is a deployment problem --a missing jar, a stray
 * system property-- that the caller has to decide how to handle. Note the contrast with {@link
 * javax.xml.stream.FactoryConfigurationError}, which for the same problem is an {@code Error}; the
 * two APIs were written with different criteria and each kept its own.
 *
 * <p>The four constructors are the usual ones. The two that take a cause pass it to {@link
 * Exception}'s constructor, so {@code getCause()} works: there is no field of its own nor manual
 * {@code initCause}.
 */
public class DatatypeConfigurationException extends Exception {

    /**
     * The same as the original's, so that a serialized instance crosses between the two libraries.
     */
    private static final long serialVersionUID = -1699373159027047238L;

    /** Without message nor cause. */
    public DatatypeConfigurationException() {
        super();
    }

    /**
     * With a message.
     *
     * @param message what happened
     */
    public DatatypeConfigurationException(String message) {
        super(message);
    }

    /**
     * With message and cause.
     *
     * @param message what happened
     * @param cause the underlying exception
     */
    public DatatypeConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * With a cause only; the message comes from it.
     *
     * @param cause the underlying exception
     */
    public DatatypeConfigurationException(Throwable cause) {
        super(cause);
    }
}
