package java.nio;

/**
 * Thrown when a write is attempted on a read-only buffer.
 *
 * <p>A read-only buffer is not a separate type — it is the same class carrying a flag — so the
 * compiler cannot catch the mistake and the check has to happen at run time. That trade is
 * deliberate: it keeps one {@code ByteBuffer} type instead of two parallel hierarchies.
 */
public class ReadOnlyBufferException extends UnsupportedOperationException {

    /**
     * The serialization identity the JDK fixed for this class. Kept literally so that a stream
     * written by either implementation is readable by the other; a value we made up would be a
     * silent incompatibility rather than a missing feature.
     */
    private static final long serialVersionUID = -1210063976496234090L;

    /** Constructs an instance with no detail message. */
    public ReadOnlyBufferException() {
        super();
    }
}
