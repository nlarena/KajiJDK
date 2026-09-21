package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedChannelException — an operation was attempted over a closed
 * channel.
 *
 * <p>It is of I/O and not of state, although it looks like the opposite: a channel can be closed for
 * causes outside the program --the other end, an interruption-- so the caller has to allow for it,
 * and hence it is checked.
 */
public class ClosedChannelException extends java.io.IOException {

    private static final long serialVersionUID = 1000000006L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ClosedChannelException() {
        super();
    }
}
