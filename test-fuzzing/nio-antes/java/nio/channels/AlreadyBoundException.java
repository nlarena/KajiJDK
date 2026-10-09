package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AlreadyBoundException — a channel was to be tied to an address
 * when it was tied already.
 *
 * <p>Tying is a once-only operation: a channel tied twice would have two addresses and no way of
 * deciding which to use.
 */
public class AlreadyBoundException extends IllegalStateException {

    private static final long serialVersionUID = 1000000001L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public AlreadyBoundException() {
        super();
    }
}
