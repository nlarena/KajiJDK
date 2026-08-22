package java.nio;

/**
 * A buffer of short values.
 *
 * <p>Every typed buffer is the same four indices as {@link Buffer} over an array of one element
 * type; what changes is only the width of an element, which is why these six classes are
 * near-identical by construction rather than by accident. In the JDK they are literally generated
 * from a single template, and so are these.
 *
 * <p>The interesting asymmetry is with {@link ByteBuffer}: bytes are what actually travel, so
 * only that class can reinterpret its contents at other widths. A {@code ShortBuffer} is either
 * allocated on its own or obtained as a <em>view</em> over some bytes — and the view is where the
 * byte order stops being an implementation detail.
 *
 * <p><strong>Omitted from this implementation</strong> (the API-shape gate requires a subset, not
 * equality): the view factories and the bulk {@code put(ShortBuffer)} form.
 */
public abstract class ShortBuffer extends Buffer implements Comparable<ShortBuffer> {

    final short[] hb;
    final int offset;
    boolean readOnly;

    ShortBuffer(short[] hb, int offset, int capacity) {
        super(capacity);
        this.hb = hb;
        this.offset = offset;
    }

    /**
     * Allocates a new buffer with the given capacity, backed by a fresh array.
     *
     * <p>The position is zero, the limit is the capacity and no mark is set.
     *
     * @param capacity the number of elements the buffer holds
     * @return the new buffer
     * @throws IllegalArgumentException if {@code capacity} is negative
     */
    public static ShortBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        return new HeapShortBuffer(new short[capacity], 0, capacity);
    }

    /**
     * Wraps an array in a buffer. The array is <em>not</em> copied: writes through the buffer are
     * visible in the array and vice versa.
     *
     * @param array the array to wrap
     * @return a buffer over {@code array}
     */
    public static ShortBuffer wrap(short[] array) {
        return new HeapShortBuffer(array, 0, array.length);
    }

    /**
     * Wraps part of an array in a buffer, positioned at {@code offset} with a limit just past
     * the given length. The array is not copied.
     *
     * @param array the array to wrap
     * @param offset the initial position
     * @param length the number of elements between the position and the limit
     * @return a buffer over {@code array}
     */
    public static ShortBuffer wrap(short[] array, int offset, int length) {
        HeapShortBuffer b = new HeapShortBuffer(array, 0, array.length);
        b.position(offset);
        b.limit(offset + length);
        return b;
    }

    /**
     * Creates a buffer over this buffer's remaining elements, sharing the backing array.
     *
     * @return the new buffer, whose position is zero and whose capacity is the remaining count
     */
    public abstract ShortBuffer slice();

    /**
     * Creates a buffer over the given range of this one, sharing the backing array.
     *
     * @param index the first element of the slice
     * @param length the number of elements in the slice
     * @return the new buffer
     */
    public abstract ShortBuffer slice(int index, int length);

    /**
     * Creates a buffer sharing this one's backing array but with its own position, limit and
     * mark — two independent cursors over one piece of memory.
     *
     * @return the new buffer
     */
    public abstract ShortBuffer duplicate();

    /**
     * Creates a read-only view of this buffer, sharing its contents.
     *
     * @return the new buffer, whose writes throw {@link ReadOnlyBufferException}
     */
    public abstract ShortBuffer asReadOnlyBuffer();

    /**
     * Reads the element at the current position and advances the position.
     *
     * @return the element read
     * @throws BufferUnderflowException if the position is at the limit
     */
    public abstract short get();

    /**
     * Reads the element at the given index without moving the position.
     *
     * @param index the index to read
     * @return the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     */
    public abstract short get(int index);

    /**
     * Writes the value at the current position and advances the position.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ShortBuffer put(short value);

    /**
     * Writes the value at the given index without moving the position.
     *
     * @param index the index to write
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ShortBuffer put(int index, short value);

    /**
     * Copies elements from this buffer into part of an array, advancing the position.
     *
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of elements to copy
     * @return this buffer
     * @throws BufferUnderflowException if fewer than {@code length} elements remain
     */
    public ShortBuffer get(short[] dst, int off, int length) {
        int from = nextGetIndex(length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + from + i];
            i = i + 1;
        }
        return this;
    }

    /**
     * Fills an array from this buffer, advancing the position.
     *
     * @param dst the array to fill
     * @return this buffer
     * @throws BufferUnderflowException if fewer than {@code dst.length} elements remain
     */
    public ShortBuffer get(short[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Copies part of an array into this buffer, advancing the position.
     *
     * @param src the array to copy from
     * @param off the first index of {@code src} to read
     * @param length the number of elements to copy
     * @return this buffer
     * @throws BufferOverflowException if fewer than {@code length} elements of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ShortBuffer put(short[] src, int off, int length) {
        checkWritable();
        int to = nextPutIndex(length);
        int i = 0;
        while (i < length) {
            hb[offset + to + i] = src[off + i];
            i = i + 1;
        }
        return this;
    }

    /**
     * Copies a whole array into this buffer, advancing the position.
     *
     * @param src the array to copy
     * @return this buffer
     * @throws BufferOverflowException if fewer than {@code src.length} elements of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ShortBuffer put(short[] src) {
        return put(src, 0, src.length);
    }

    /**
     * Slides the unread elements to the front and positions after them.
     *
     * <p>This is the operation that turns a fixed buffer into a window over an unbounded stream:
     * consume part of it, compact, refill the rest.
     *
     * @return this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ShortBuffer compact() {
        checkWritable();
        int n = remaining();
        int from = position();
        int i = 0;
        while (i < n) {
            hb[offset + i] = hb[offset + from + i];
            i = i + 1;
        }
        position(n);
        limit(capacity());
        return this;
    }

    /**
     * Tells whether this buffer's memory lives outside the Java heap.
     *
     * @return always {@code false} — KajiLibrary has no off-heap allocation
     */
    public boolean isDirect() {
        return false;
    }

    /**
     * Tells whether this buffer exposes an accessible backing array.
     *
     * @return {@code true} unless this buffer is read-only
     */
    public boolean hasArray() {
        return !readOnly;
    }

    /**
     * Returns the backing array.
     *
     * @return the array this buffer reads and writes
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public Object array() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
        return (Object) hb;
    }

    /**
     * Returns the index in the backing array of this buffer's first element.
     *
     * @return the offset of element zero within the array
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public int arrayOffset() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
        return offset;
    }

    /**
     * Returns this buffer's byte order.
     *
     * <p>A standalone typed buffer has the platform's order; only a <em>view</em> over a
     * {@link ByteBuffer} carries the order of the bytes underneath.
     *
     * @return the byte order
     */
    public abstract ByteOrder order();

    final void checkWritable() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
    }

    /**
     * Compares this buffer with another for equality of their <em>remaining</em> elements —
     * what is left to read, not the whole array and not the indices.
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is a {@code ShortBuffer} with the same
     *         remaining contents
     */
    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof ShortBuffer) {
            ShortBuffer that = (ShortBuffer) other;
            if (remaining() == that.remaining()) {
                same = true;
                int i = 0;
                while (i < remaining()) {
                    if (get(position() + i) != that.get(that.position() + i)) {
                        same = false;
                    }
                    i = i + 1;
                }
            }
        }
        return same;
    }

    /**
     * Returns a hash code over the remaining elements, consistent with {@link #equals}.
     *
     * @return the hash code
     */
    public int hashCode() {
        int h = 1;
        int i = position();
        while (i < limit()) {
            h = 31 * h + (int) get(i);
            i = i + 1;
        }
        return h;
    }

    /**
     * Compares the remaining elements of the two buffers lexicographically.
     *
     * @param that the buffer to compare with
     * @return a negative number, zero or a positive number as this buffer is less than, equal to
     *         or greater than {@code that}
     */
    public int compareTo(ShortBuffer that) {
        int n = remaining();
        if (that.remaining() < n) {
            n = that.remaining();
        }
        int result = remaining() - that.remaining();
        int i = 0;
        boolean decided = false;
        while (i < n && !decided) {
            short a = get(position() + i);
            short b = that.get(that.position() + i);
            if (a != b) {
                result = -1;
                if (a > b) {
                    result = 1;
                }
                decided = true;
            }
            i = i + 1;
        }
        return result;
    }

    /**
     * Returns a summary of this buffer's indices — not its contents.
     *
     * @return a string of the form {@code ShortBuffer[pos=.. lim=.. cap=..]}
     */
    public String toString() {
        return "ShortBuffer[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]";
    }
}

/**
 * The heap implementation of {@link ShortBuffer}. Top-level and package-private rather than
 * nested — the project's idiom for a helper type — and skipped by the API-shape gate, which has
 * no JDK counterpart to compare it against.
 */
class HeapShortBuffer extends ShortBuffer {

    HeapShortBuffer(short[] hb, int offset, int capacity) {
        super(hb, offset, capacity);
    }

    public ShortBuffer slice() {
        HeapShortBuffer b = new HeapShortBuffer(hb, offset + position(), remaining());
        b.readOnly = readOnly;
        return b;
    }

    public ShortBuffer slice(int index, int length) {
        HeapShortBuffer b = new HeapShortBuffer(hb, offset + index, length);
        b.readOnly = readOnly;
        return b;
    }

    public ShortBuffer duplicate() {
        HeapShortBuffer b = new HeapShortBuffer(hb, offset, capacity());
        b.position(position());
        b.limit(limit());
        b.readOnly = readOnly;
        return b;
    }

    public ShortBuffer asReadOnlyBuffer() {
        HeapShortBuffer b = (HeapShortBuffer) duplicate();
        b.readOnly = true;
        return b;
    }

    /**
     * Tells whether this buffer rejects writes.
     *
     * <p>Declared here rather than on the abstract buffer: the JDK leaves it abstract all
     * the way from {@link Buffer}, so declaring it higher up would be a member the gate
     * reports as extra.
     *
     * @return {@code true} if this buffer is read-only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public short get() {
        return hb[offset + nextGetIndex()];
    }

    public short get(int index) {
        return hb[offset + checkIndex(index)];
    }

    public ShortBuffer put(short value) {
        checkWritable();
        hb[offset + nextPutIndex()] = value;
        return this;
    }

    public ShortBuffer put(int index, short value) {
        checkWritable();
        hb[offset + checkIndex(index)] = value;
        return this;
    }

    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}
