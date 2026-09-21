package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.FileLockInterruptionException — the thread was interrupted while
 * it waited to acquire a file lock.
 *
 * <p>Unlike `ClosedByInterruptException`, the channel is **not** closed: waiting for a lock leaves
 * nothing half done, so abandoning the wait is enough.
 */
public class FileLockInterruptionException extends java.io.IOException {

    private static final long serialVersionUID = 1000000009L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public FileLockInterruptionException() {
        super();
    }
}
