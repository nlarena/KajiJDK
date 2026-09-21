package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalBlockingModeException — the operation asked for is not
 * valid in the blocking mode the channel is in.
 *
 * <p>A channel in non-blocking mode cannot do a read that waits, and one in blocking mode cannot be
 * registered in a selector. They are not arbitrary limitations: registering a blocking channel
 * would make the selector get stuck in it, which is exactly what a selector exists to avoid.
 */
public class IllegalBlockingModeException extends IllegalStateException {

    private static final long serialVersionUID = 1000000010L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public IllegalBlockingModeException() {
        super();
    }
}
