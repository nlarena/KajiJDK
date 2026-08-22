package java.nio;

/**
 * The state every buffer shares: four indices and a single invariant.
 *
 * <blockquote>{@code 0 <= mark <= position <= limit <= capacity}</blockquote>
 *
 * <p>{@code capacity} is fixed when the buffer is created. {@code limit} is the first element
 * that must not be read or written. {@code position} is the cursor. {@code mark} is a remembered
 * position, or −1 when unset.
 *
 * <p>Everything else in this package is a consequence of those four. {@link #flip()} — limit
 * becomes position, position becomes zero — is what turns a buffer you were <em>writing</em> into
 * one you are <em>reading</em>, and it is the single most characteristic operation of the API:
 * the same object, the same array, no copy, just two indices reinterpreted. {@link #clear()} does
 * not erase anything either; it resets the indices so that writing can start over. Both names
 * mislead every reader exactly once.
 *
 * <p>The mark is discarded whenever the position or limit moves back past it, which is why it
 * belongs to the buffer rather than to the caller — see {@link InvalidMarkException}.
 */
public abstract class Buffer {

    private final int capacity;
    private int limit;
    private int position;
    private int mark;

    Buffer(int capacity) {
        this.capacity = capacity;
        this.limit = capacity;
        this.position = 0;
        this.mark = -1;
    }

    /**
     * Returns the number of elements this buffer holds. Fixed at creation.
     *
     * @return the capacity
     */
    public final int capacity() {
        return capacity;
    }

    /**
     * Returns the index of the next element to be read or written.
     *
     * @return the position
     */
    public final int position() {
        return position;
    }

    /**
     * Sets the position. If the mark is now beyond it, the mark is discarded.
     *
     * @param newPosition the new position, between zero and the limit
     * @return this buffer
     * @throws IllegalArgumentException if {@code newPosition} is negative or past the limit
     */
    public Buffer position(int newPosition) {
        if (newPosition > limit || newPosition < 0) {
            throw new IllegalArgumentException("position out of bounds");
        }
        position = newPosition;
        if (mark > position) {
            mark = -1;
        }
        return this;
    }

    /**
     * Returns the index of the first element that must not be read or written.
     *
     * @return the limit
     */
    public final int limit() {
        return limit;
    }

    /**
     * Sets the limit, pulling the position and mark back with it if they would be left beyond.
     *
     * @param newLimit the new limit, between zero and the capacity
     * @return this buffer
     * @throws IllegalArgumentException if {@code newLimit} is negative or past the capacity
     */
    public Buffer limit(int newLimit) {
        if (newLimit > capacity || newLimit < 0) {
            throw new IllegalArgumentException("limit out of bounds");
        }
        limit = newLimit;
        if (position > limit) {
            position = limit;
        }
        if (mark > limit) {
            mark = -1;
        }
        return this;
    }

    /**
     * Remembers the current position so that {@link #reset()} can return to it.
     *
     * @return this buffer
     */
    public Buffer mark() {
        mark = position;
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     * @throws InvalidMarkException if no mark is set, or the mark was discarded
     */
    public Buffer reset() {
        if (mark < 0) {
            throw new InvalidMarkException();
        }
        position = mark;
        return this;
    }

    /**
     * Prepares the buffer to be written from scratch: position zero, limit at the capacity, no
     * mark. Nothing is erased — only the indices change.
     *
     * @return this buffer
     */
    public Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    /**
     * Turns a written buffer into a readable one: the limit is set to the current position and
     * the position to zero. The whole API in one line.
     *
     * @return this buffer
     */
    public Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * Rereads what was already read: the position goes back to zero and the limit is untouched.
     *
     * @return this buffer
     */
    public Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * Returns how many elements lie between the position and the limit.
     *
     * @return the number of elements remaining
     */
    public final int remaining() {
        return limit - position;
    }

    /**
     * Tells whether any element lies between the position and the limit.
     *
     * @return {@code true} if at least one element remains
     */
    public final boolean hasRemaining() {
        return position < limit;
    }

    /**
     * Tells whether this buffer rejects writes.
     *
     * @return {@code true} if this buffer is read-only
     */
    public abstract boolean isReadOnly();

    /**
     * Tells whether this buffer exposes an accessible backing array. A read-only or direct buffer
     * does not, which is why the array accessors are not simply always available.
     *
     * @return {@code true} if {@link #array()} may be called
     */
    public abstract boolean hasArray();

    /**
     * Returns the backing array.
     *
     * @return the array this buffer reads and writes
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract Object array();

    /**
     * Returns the index in the backing array of this buffer's first element.
     *
     * @return the offset of element zero within the array
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract int arrayOffset();

    /**
     * Tells whether this buffer's memory lives outside the Java heap, where I/O can hand it to the
     * operating system without copying.
     *
     * @return always {@code false} here — KajiLibrary has no off-heap allocation
     */
    public abstract boolean isDirect();

    /**
     * Creates a buffer over this buffer's remaining elements, sharing the backing memory.
     *
     * @return the new buffer
     */
    public abstract Buffer slice();

    /**
     * Creates a buffer over the given range of this one, sharing the backing memory.
     *
     * @param index the first element of the slice
     * @param length the number of elements in the slice
     * @return the new buffer
     */
    public abstract Buffer slice(int index, int length);

    /**
     * Creates a buffer sharing this one's backing memory but with its own indices.
     *
     * @return the new buffer
     */
    public abstract Buffer duplicate();

    // ---- helpers para las subclases ----

    /** The index for a relative read of one element, advancing the position. */
    final int nextGetIndex() {
        if (position >= limit) {
            throw new BufferUnderflowException();
        }
        int at = position;
        position = position + 1;
        return at;
    }

    /** The index for a relative write of one element, advancing the position. */
    final int nextPutIndex() {
        if (position >= limit) {
            throw new BufferOverflowException();
        }
        int at = position;
        position = position + 1;
        return at;
    }

    /** The index for a relative read of {@code count} elements, advancing the position. */
    final int nextGetIndex(int count) {
        if (limit - position < count) {
            throw new BufferUnderflowException();
        }
        int at = position;
        position = position + count;
        return at;
    }

    /** The index for a relative write of {@code count} elements, advancing the position. */
    final int nextPutIndex(int count) {
        if (limit - position < count) {
            throw new BufferOverflowException();
        }
        int at = position;
        position = position + count;
        return at;
    }

    /**
     * Validates an absolute index, which does NOT move the position — the other half of the API,
     * and the half that lets a buffer be used from more than one place at a time.
     */
    final int checkIndex(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        return index;
    }

    /** Validates an absolute range of {@code count} elements starting at {@code index}. */
    final int checkIndex(int index, int count) {
        if (index < 0 || count < 0 || index + count > limit) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        return index;
    }
}
