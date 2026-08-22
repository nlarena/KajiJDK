package java.nio;

/**
 * Thrown by a relative <em>get</em> when nothing remains between the position and the limit.
 *
 * <p>The mirror of {@link BufferOverflowException}, and unchecked for the same reason.
 */
public class BufferUnderflowException extends RuntimeException {

    /** Constructs an instance with no detail message. */
    public BufferUnderflowException() {
        super();
    }
}
