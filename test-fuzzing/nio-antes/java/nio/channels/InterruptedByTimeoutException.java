package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.InterruptedByTimeoutException — an asynchronous operation was
 * abandoned because its deadline ran out.
 *
 * <p>It says that the deadline ran out, not that the operation failed: it may have been left half
 * done on the other side. It is the difference between 'it did not happen' and 'I do not know
 * whether it happened', and the caller has to treat it as the second.
 */
public class InterruptedByTimeoutException extends java.io.IOException {

    private static final long serialVersionUID = 1000000013L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public InterruptedByTimeoutException() {
        super();
    }
}
