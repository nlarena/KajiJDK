package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.TransformerConfigurationException -- nothing could be *set up*.
 *
 * <p>It separates two failures that look alike and are fixed differently. A plain {@link
 * TransformerException} says the transformation failed; this one says it **did not even get to
 * start**: the stylesheet does not compile, the factory does not support a feature it was asked
 * for, the `Templates` could not be built. One is investigated by looking at the input document;
 * the other, by looking at the configuration. That is why it is a type of its own and not a
 * different message.
 *
 * <p>The no-argument constructor sets {@code "Configuration Error"} as the message instead of
 * leaving it null. It is not decorative: an exception without a message showing up in a log at
 * three in the morning says absolutely nothing, and here the class name is already all the
 * information there is.
 */
public class TransformerConfigurationException extends TransformerException {

    private static final long serialVersionUID = -4251405565727967249L;

    /** Without data; the message stays {@code "Configuration Error"}. */
    public TransformerConfigurationException() {
        super("Configuration Error");
    }

    /**
     * With a message.
     *
     * @param msg the description of the error
     */
    public TransformerConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Wrapping another exception.
     *
     * @param e the cause
     */
    public TransformerConfigurationException(Throwable e) {
        super(e);
    }

    /**
     * With message and cause.
     *
     * @param msg the description of the error
     * @param e the cause
     */
    public TransformerConfigurationException(String msg, Throwable e) {
        super(msg, e);
    }

    /**
     * With message and location.
     *
     * @param msg the description of the error
     * @param locator where it happened
     */
    public TransformerConfigurationException(String msg, SourceLocator locator) {
        super(msg, locator);
    }

    /**
     * With message, location and cause.
     *
     * @param msg the description of the error
     * @param locator where it happened
     * @param e the cause
     */
    public TransformerConfigurationException(String msg, SourceLocator locator, Throwable e) {
        super(msg, locator, e);
    }
}
