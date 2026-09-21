package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedByInterruptException — the thread that was blocked in an I/O
 * operation was **interrupted**, and that is why the channel was closed.
 *
 * <p>The closing is not an untidy side effect but the semantics of an interruptible channel: an I/O
 * operation half done cannot be left half done, so interrupting closes. That is why it extends
 * `AsynchronousCloseException` --the channel was left closed-- and not a plain interruption
 * exception.
 */
public class ClosedByInterruptException extends AsynchronousCloseException {

    private static final long serialVersionUID = 1000000005L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ClosedByInterruptException() {
        super();
    }
}
