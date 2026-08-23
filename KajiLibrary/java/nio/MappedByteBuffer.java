package java.nio;

/**
 * A byte buffer whose contents are a memory-mapped region of a file.
 *
 * <p>Mapping is the point of this class: the operating system makes a file's bytes appear as
 * memory, so reading the buffer reads the file without a copy through the Java heap, and the
 * kernel pages content in and out on demand. That is a capability of the OS reached through the
 * VM, not something a library can synthesise.
 *
 * <p><strong>KajiLibrary cannot map anything.</strong> There is no {@code java.nio.channels},
 * no {@code FileChannel} and no file descriptors, so no instance of this class can be created —
 * the only constructor is package-private and nothing calls it. What is declared here is the
 * surface a caller compiles against; the methods describe what they would do.
 *
 * <p>This is the same shape as {@code ZipFile} in {@code java.util.zip}: an honest hole is better
 * than a class that pretends to hold something it does not.
 *
 * <p>Note what the class does <em>not</em> hide. Everything about the indices is re-declared here
 * only to narrow the return type to {@code MappedByteBuffer}, and every one of those overrides is
 * {@code final} — a mapped buffer's indices behave exactly like any other buffer's. The four that
 * stay {@code abstract} ({@code slice}, {@code duplicate}, {@code compact}) are the ones that have
 * to produce another mapped buffer, which only an implementation that owns a mapping can do.
 */
public abstract class MappedByteBuffer extends ByteBuffer {

    /**
     * Whether this mapping is over a volatile (persistent-memory) file, in which case
     * {@link #force()} has to write back with cache-line instructions instead of an {@code msync}.
     * Always {@code false} here — there is no mapping to be synchronous about.
     */
    private final boolean isSync;

    MappedByteBuffer(byte[] hb, int offset, int capacity) {
        super(hb, offset, capacity);
        this.isSync = false;
    }

    /**
     * Tells whether this buffer maps a file on persistent memory.
     *
     * <p>Package-private because it is not a question a caller can act on: it selects which
     * write-back instruction {@link #force()} would use, and both branches are unreachable here.
     *
     * @return {@code false}
     */
    final boolean isSync() {
        return isSync;
    }

    /**
     * Tells whether this buffer's content is resident in physical memory.
     *
     * @return {@code false}; without a mapping there is nothing resident
     */
    public final boolean isLoaded() {
        return false;
    }

    /**
     * Attempts to load this buffer's content into physical memory.
     *
     * @return this buffer, unchanged
     */
    public final MappedByteBuffer load() {
        return this;
    }

    /**
     * Writes any changes back to the file that contains the mapped region.
     *
     * @return this buffer, unchanged
     */
    public final MappedByteBuffer force() {
        return this;
    }

    /**
     * Writes back only the changes in the given range — the reason the method has an overload at
     * all: forcing a whole large mapping is expensive, and a writer usually knows which window it
     * dirtied.
     *
     * @param index the first byte of the range
     * @param length the number of bytes in the range
     * @return this buffer, unchanged
     * @throws IndexOutOfBoundsException if the range falls outside this buffer's capacity
     */
    public final MappedByteBuffer force(int index, int length) {
        if (index < 0 || length < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException("range out of bounds for capacity " + capacity());
        }
        return this;
    }

    /**
     * Creates a mapped buffer over this one's remaining bytes.
     *
     * @return the new buffer
     */
    public abstract MappedByteBuffer slice();

    /**
     * Creates a mapped buffer over the given range of this one.
     *
     * @param index the first byte of the slice
     * @param length the number of bytes in the slice
     * @return the new buffer
     */
    public abstract MappedByteBuffer slice(int index, int length);

    /**
     * Creates a mapped buffer sharing this one's mapping but with its own indices.
     *
     * @return the new buffer
     */
    public abstract MappedByteBuffer duplicate();

    /**
     * Slides the unread bytes to the front and positions after them.
     *
     * @return this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract MappedByteBuffer compact();

    /**
     * Sets the position.
     *
     * <p>As in the typed buffers, these seven overrides only narrow the return type; the body
     * calls the package-private worker in {@link Buffer} because our javac does not emit
     * {@code super.method()} yet.
     *
     * @param newPosition the new position
     * @return this buffer
     */
    public final MappedByteBuffer position(int newPosition) {
        setPosition(newPosition);
        return this;
    }

    /**
     * Sets the limit.
     *
     * @param newLimit the new limit
     * @return this buffer
     */
    public final MappedByteBuffer limit(int newLimit) {
        setLimit(newLimit);
        return this;
    }

    /**
     * Remembers the current position.
     *
     * @return this buffer
     */
    public final MappedByteBuffer mark() {
        setMark();
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     */
    public final MappedByteBuffer reset() {
        resetToMark();
        return this;
    }

    /**
     * Resets the indices for writing from scratch.
     *
     * @return this buffer
     */
    public final MappedByteBuffer clear() {
        clearIndices();
        return this;
    }

    /**
     * Turns a written buffer into a readable one.
     *
     * @return this buffer
     */
    public final MappedByteBuffer flip() {
        flipIndices();
        return this;
    }

    /**
     * Rewinds to position zero, keeping the limit.
     *
     * @return this buffer
     */
    public final MappedByteBuffer rewind() {
        rewindIndices();
        return this;
    }
}
