package java.nio;

/**
 * Thrown by a relative <em>get</em> when nothing remains between the position and the limit.
 *
 * <p>The mirror of {@link BufferOverflowException}, and unchecked for the same reason.
 */
public class BufferUnderflowException extends RuntimeException {

    /**
     * The serialization identity the JDK fixed for this class. Kept literally so that a stream
     * written by either implementation is readable by the other; a value we made up would be a
     * silent incompatibility rather than a missing feature.
     */
    private static final long serialVersionUID = -1713313658691622206L;

    /** Constructs an instance with no detail message. */
    public BufferUnderflowException() {
        super();
    }
}
