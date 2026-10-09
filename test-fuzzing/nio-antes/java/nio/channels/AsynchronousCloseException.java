package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AsynchronousCloseException — the channel was closed by **another
 * thread** while this operation was under way.
 *
 * <p>It extends `ClosedChannelException` and adds the part that matters for diagnosing: the channel
 * was not closed when the operation started. Whoever receives it knows they did not get the order
 * wrong; the channel was closed underneath them.
 */
public class AsynchronousCloseException extends ClosedChannelException {

    private static final long serialVersionUID = 1000000003L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public AsynchronousCloseException() {
        super();
    }
}
