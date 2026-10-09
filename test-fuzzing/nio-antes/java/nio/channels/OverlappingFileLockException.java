package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.OverlappingFileLockException — a lock was asked for over a region
 * of file that is **already** locked by this same VM, or there is another request under way over
 * it.
 *
 * <p>It is inside the same VM on purpose: file locks belong to the process before the operating
 * system, so two parts of the same program do not block each other --they step on each other--.
 * This exception is what turns that silent stepping into a visible error.
 */
public class OverlappingFileLockException extends IllegalStateException {

    private static final long serialVersionUID = 1000000019L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public OverlappingFileLockException() {
        super();
    }
}
