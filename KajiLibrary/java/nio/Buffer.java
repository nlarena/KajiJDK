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
        setPosition(newPosition);
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
        setLimit(newLimit);
        return this;
    }

    /**
     * Remembers the current position so that {@link #reset()} can return to it.
     *
     * @return this buffer
     */
    public Buffer mark() {
        setMark();
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     * @throws InvalidMarkException if no mark is set, or the mark was discarded
     */
    public Buffer reset() {
        resetToMark();
        return this;
    }

    /**
     * Prepares the buffer to be written from scratch: position zero, limit at the capacity, no
     * mark. Nothing is erased — only the indices change.
     *
     * @return this buffer
     */
    public Buffer clear() {
        clearIndices();
        return this;
    }

    /**
     * Turns a written buffer into a readable one: the limit is set to the current position and
     * the position to zero. The whole API in one line.
     *
     * @return this buffer
     */
    public Buffer flip() {
        flipIndices();
        return this;
    }

    /**
     * Rereads what was already read: the position goes back to zero and the limit is untouched.
     *
     * @return this buffer
     */
    public Buffer rewind() {
        rewindIndices();
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
     * Returns the object this buffer's elements actually live in, or {@code null} when there is
     * none.
     *
     * <p>Not the same question as {@link #array()}, which is the caller-facing accessor and
     * refuses to answer for a read-only buffer. This one is the implementation's own handle on
     * the storage and always tells the truth: the backing array for a heap buffer, {@code null}
     * for a view over a {@link ByteBuffer} — a view owns nothing, it decodes.
     *
     * @return the backing object, or {@code null}
     */
    abstract Object base();

    /**
     * Returns how far an element index has to be shifted left to become a byte offset — 0 for a
     * byte buffer, 1 for {@code char} and {@code short}, 2 for {@code int} and {@code float},
     * 3 for {@code long} and {@code double}.
     *
     * <p>A shift rather than a multiplication because every element width in the language is a
     * power of two, which is also why the bulk operations can be written once for all six types.
     *
     * @return the base-two logarithm of this buffer's element size in bytes
     */
    abstract int scaleShifts();

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

    // ---- el trabajo real de los siete mutadores de indices ----
    //
    // Cada subclase tipada vuelve a declarar `position(int)`, `limit(int)`, `mark()`, `reset()`,
    // `clear()`, `flip()` y `rewind()` para devolver SU tipo (asi una cadena de llamadas no se
    // degrada a `Buffer`). En el JDK esos override llaman a `super.position(...)`; nuestro javac
    // todavia no genera `invokespecial` para `super.metodo()` ("el generador de bytecode todavia
    // no soporta `super`"), asi que el cuerpo vive aca, en metodos `final` que nadie sobreescribe,
    // y tanto Buffer como las subclases los invocan por nombre. Es el mismo codigo una sola vez;
    // lo unico que se pierde es la forma de escribirlo.

    final void setPosition(int newPosition) {
        if (newPosition > limit || newPosition < 0) {
            throw createPositionException(newPosition);
        }
        position = newPosition;
        if (mark > position) {
            mark = -1;
        }
    }

    final void setLimit(int newLimit) {
        if (newLimit > capacity || newLimit < 0) {
            throw createLimitException(newLimit);
        }
        limit = newLimit;
        if (position > limit) {
            position = limit;
        }
        if (mark > limit) {
            mark = -1;
        }
    }

    final void setMark() {
        mark = position;
    }

    final void resetToMark() {
        if (mark < 0) {
            throw new InvalidMarkException();
        }
        position = mark;
    }

    final void clearIndices() {
        position = 0;
        limit = capacity;
        mark = -1;
    }

    final void flipIndices() {
        limit = position;
        position = 0;
        mark = -1;
    }

    final void rewindIndices() {
        position = 0;
        mark = -1;
    }

    /**
     * The exception for "you handed a buffer to itself", which every bulk {@code put} has to
     * reject: the copy would read the same elements it is overwriting.
     */
    static IllegalArgumentException createSameBufferException() {
        return new IllegalArgumentException("The source buffer is this buffer");
    }

    /** The exception for a negative capacity, shared by every {@code allocate}. */
    static IllegalArgumentException createCapacityException(int capacity) {
        return new IllegalArgumentException("capacity < 0: (" + capacity + " < 0)");
    }

    private IllegalArgumentException createPositionException(int newPosition) {
        String reason;
        if (newPosition > limit) {
            reason = "newPosition > limit: (" + newPosition + " > " + limit + ")";
        } else {
            reason = "newPosition < 0: (" + newPosition + " < 0)";
        }
        return new IllegalArgumentException(reason);
    }

    private IllegalArgumentException createLimitException(int newLimit) {
        String reason;
        if (newLimit > capacity) {
            reason = "newLimit > capacity: (" + newLimit + " > " + capacity + ")";
        } else {
            reason = "newLimit < 0: (" + newLimit + " < 0)";
        }
        return new IllegalArgumentException(reason);
    }

    /** The current mark, or −1 if none — what a subclass has to carry into a duplicate. */
    final int markValue() {
        return mark;
    }

    /** Forgets the mark. */
    final void discardMark() {
        mark = -1;
    }

    /**
     * Validates a range against a plain array's length, for the bulk accessors.
     *
     * <p>The single-comparison trick is the JDK's: a negative offset, a negative length, an
     * overflowing sum or a range past the end all set the sign bit of the {@code or}.
     */
    static void checkBounds(int off, int len, int size) {
        if ((off | len | (off + len) | (size - (off + len))) < 0) {
            throw new IndexOutOfBoundsException("range out of bounds for array of length " + size);
        }
    }

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
