package java.nio;

/**
 * Thrown by a relative <em>put</em> when the buffer's limit has been reached.
 *
 * <p>Unchecked, because it reports a programming error rather than a condition to recover from:
 * the caller is expected to have checked {@link Buffer#remaining()} or to have sized the buffer.
 */
public class BufferOverflowException extends RuntimeException {

    /** Constructs an instance with no detail message. */
    public BufferOverflowException() {
        super();
    }
}
