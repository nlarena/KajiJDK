package java.nio;

/**
 * Thrown by a relative <em>put</em> when the buffer's limit has been reached.
 *
 * <p>Unchecked, because it reports a programming error rather than a condition to recover from:
 * the caller is expected to have checked {@link Buffer#remaining()} or to have sized the buffer.
 */
public class BufferOverflowException extends RuntimeException {

    /**
     * The serialization identity the JDK fixed for this class. Kept literally so that a stream
     * written by either implementation is readable by the other; a value we made up would be a
     * silent incompatibility rather than a missing feature.
     */
    private static final long serialVersionUID = -5484897634319144535L;

    /** Constructs an instance with no detail message. */
    public BufferOverflowException() {
        super();
    }
}
