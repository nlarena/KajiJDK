package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamException -- what is thrown when reading or writing XML
 * cannot go on.
 *
 * <p>It is a **checked** exception, and that is on purpose: almost every method of {@link
 * XMLStreamReader} and {@link XMLStreamWriter} declares it, so the compiler forces a decision about
 * what to do with a malformed document. The alternative --an unchecked one-- would turn the most
 * common case of all (invalid input) into something discovered in production.
 *
 * <h2>Two protected fields that would not be written today</h2>
 *
 * <p>{@link #nested} and {@link #location} are {@code protected} because the class is from 2004 and
 * {@code Throwable} had only just gained chained causes. Today {@link #nested} would be redundant
 * with {@link Throwable#getCause()} --and in fact the constructors that receive it put it on both
 * sides-- but it is kept as is because there are subclasses outside that read it.
 *
 * <p>The asymmetry that does surprise and that is reproduced: the {@linkplain
 * #XMLStreamException(String, Location) message plus location} constructor leaves {@code
 * getCause()} null, while the {@linkplain #XMLStreamException(String, Location, Throwable) message,
 * location and cause} one sets it. It is not an oversight here but the contract's behaviour.
 *
 * <h2>The message is built, not kept</h2>
 *
 * <p>The two constructors that receive a {@link Location} do not keep the message they were given:
 * they wrap it in {@code "ParseError at [row,col]:[L,C]\nMessage: ..."} before passing it to {@code
 * super}. It is ugly and comes out on two lines, but it is the text StAX users already know and
 * that tools parse, so it is copied to the character.
 */
public class XMLStreamException extends Exception {

    /**
     * The inner exception, if there is one.
     *
     * <p>It duplicates {@link Throwable#getCause()} when the constructor received both; see the
     * header.
     */
    protected Throwable nested;

    /** Where it happened, or null if whoever threw did not know. */
    protected Location location;

    /** Without message, cause or location. */
    public XMLStreamException() {
        super();
    }

    /**
     * With a message only.
     *
     * @param msg the message
     */
    public XMLStreamException(String msg) {
        super(msg);
    }

    /**
     * Wrapping another exception.
     *
     * <p>The resulting message is the wrapped one's {@code toString()}, which is what
     * {@code Throwable(Throwable)} does.
     *
     * @param th the inner exception
     */
    public XMLStreamException(Throwable th) {
        super(th);
        nested = th;
    }

    /**
     * With its own message and a cause.
     *
     * @param msg the message
     * @param th the inner exception
     */
    public XMLStreamException(String msg, Throwable th) {
        super(msg, th);
        nested = th;
    }

    /**
     * With message, location and cause.
     *
     * @param msg the message, which is wrapped in the {@code ParseError at [row,col]} format
     * @param location where it happened
     * @param th the inner exception
     */
    public XMLStreamException(String msg, Location location, Throwable th) {
        super("ParseError at [row,col]:[" + location.getLineNumber() + ","
                + location.getColumnNumber() + "]\nMessage: " + msg, th);
        nested = th;
        this.location = location;
    }

    /**
     * With message and location.
     *
     * <p>It leaves {@code getCause()} and {@link #getNestedException()} null; see the header.
     *
     * @param msg the message, which is wrapped in the {@code ParseError at [row,col]} format
     * @param location where it happened
     */
    public XMLStreamException(String msg, Location location) {
        super("ParseError at [row,col]:[" + location.getLineNumber() + ","
                + location.getColumnNumber() + "]\nMessage: " + msg);
        this.location = location;
    }

    /**
     * The wrapped exception, or null.
     *
     * @return the inner one
     */
    public Throwable getNestedException() {
        return nested;
    }

    /**
     * Where it happened, or null if not known.
     *
     * @return the location
     */
    public Location getLocation() {
        return location;
    }
}
