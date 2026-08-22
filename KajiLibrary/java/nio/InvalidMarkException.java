package java.nio;

/**
 * Thrown by {@link Buffer#reset()} when no mark is set.
 *
 * <p>A mark is discarded whenever the position or limit moves back past it, so a mark that was
 * set earlier may legitimately be gone by the time it is used. That is precisely why the mark
 * lives inside the buffer instead of being an index the caller keeps: an index the caller kept
 * would survive a {@link Buffer#flip()} and silently point at the wrong element.
 */
public class InvalidMarkException extends IllegalStateException {

    /** Constructs an instance with no detail message. */
    public InvalidMarkException() {
        super();
    }
}
