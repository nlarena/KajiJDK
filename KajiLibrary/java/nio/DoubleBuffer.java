package java.nio;

/**
 * A buffer of double values.
 *
 * <p>Every typed buffer is the same four indices as {@link Buffer} over an array of one element
 * type; what changes is only the width of an element, which is why these six classes are
 * near-identical by construction rather than by accident. In the JDK they are literally generated
 * from a single template, and so are these.
 *
 * <p>The interesting asymmetry is with {@link ByteBuffer}: bytes are what actually travel, so
 * only that class can reinterpret its contents at other widths. A {@code DoubleBuffer} is either
 * allocated on its own — and then it owns a {@code double[]} — or obtained as a <em>view</em> over
 * some bytes via {@link ByteBuffer#asDoubleBuffer()}, and then it owns nothing and every access is
 * decoded from the bytes underneath. That second case is why {@link #hb} may be {@code null} and
 * why the bulk operations below are written in terms of {@link #get(int)} and
 * {@link #put(int, double)} rather than reaching into the array: a view has no array to reach into.
 *
 * <p><strong>Omitted from this implementation</strong> (the API-shape gate requires a subset, not
 * equality): everything that needs {@code Unsafe}, {@code MemorySegment} or a scoped session —
 * {@code heapSegment(...)} and the {@code MemorySegment} constructors — because there is no
 * off-heap memory here at all.
 */
public abstract class DoubleBuffer extends Buffer implements Comparable<DoubleBuffer> {

    /**
     * The backing array, or {@code null} for a view over a {@link ByteBuffer}. Every accessor
     * that touches it has to ask first; that is the whole cost of supporting views.
     */
    final double[] hb;
    final int offset;
    boolean isReadOnly;

    DoubleBuffer(double[] hb, int offset, int capacity) {
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
    public static DoubleBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw Buffer.createCapacityException(capacity);
        }
        return new HeapDoubleBuffer(new double[capacity], 0, capacity);
    }

    /**
     * Wraps an array in a buffer. The array is <em>not</em> copied: writes through the buffer are
     * visible in the array and vice versa.
     *
     * @param array the array to wrap
     * @return a buffer over {@code array}
     */
    public static DoubleBuffer wrap(double[] array) {
        return new HeapDoubleBuffer(array, 0, array.length);
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
    public static DoubleBuffer wrap(double[] array, int offset, int length) {
        HeapDoubleBuffer b = new HeapDoubleBuffer(array, 0, array.length);
        b.position(offset);
        b.limit(offset + length);
        return b;
    }

    /**
     * Creates a buffer over this buffer's remaining elements, sharing the backing memory.
     *
     * @return the new buffer, whose position is zero and whose capacity is the remaining count
     */
    public abstract DoubleBuffer slice();

    /**
     * Creates a buffer over the given range of this one, sharing the backing memory.
     *
     * @param index the first element of the slice
     * @param length the number of elements in the slice
     * @return the new buffer
     */
    public abstract DoubleBuffer slice(int index, int length);

    /**
     * Creates a buffer sharing this one's backing memory but with its own position, limit and
     * mark — two independent cursors over one piece of memory.
     *
     * @return the new buffer
     */
    public abstract DoubleBuffer duplicate();

    /**
     * Creates a read-only view of this buffer, sharing its contents.
     *
     * @return the new buffer, whose writes throw {@link ReadOnlyBufferException}
     */
    public abstract DoubleBuffer asReadOnlyBuffer();

    /**
     * Reads the element at the current position and advances the position.
     *
     * @return the element read
     * @throws BufferUnderflowException if the position is at the limit
     */
    public abstract double get();

    /**
     * Reads the element at the given index without moving the position.
     *
     * @param index the index to read
     * @return the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     */
    public abstract double get(int index);

    /**
     * Writes the value at the current position and advances the position.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract DoubleBuffer put(double value);

    /**
     * Writes the value at the given index without moving the position.
     *
     * @param index the index to write
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract DoubleBuffer put(int index, double value);

    /**
     * Copies elements from this buffer into part of an array, advancing the position.
     *
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of elements to copy
     * @return this buffer
     * @throws BufferUnderflowException if fewer than {@code length} elements remain
     */
    public DoubleBuffer get(double[] dst, int off, int length) {
        Buffer.checkBounds(off, length, dst.length);
        int from = nextGetIndex(length);
        int i = 0;
        while (i < length) {
            dst[off + i] = get(from + i);
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
    public DoubleBuffer get(double[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Copies elements starting at an absolute index into part of an array. The position does
     * not move — this is the bulk form of {@link #get(int)}, and the reason a single buffer can
     * be read from more than one place at a time.
     *
     * @param index the first element of this buffer to read
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of elements to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its container
     */
    public DoubleBuffer get(int index, double[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        return getArray(index, dst, off, length);
    }

    /** The unchecked copy behind the absolute bulk {@code get}. */
    private DoubleBuffer getArray(int index, double[] dst, int off, int length) {
        int i = 0;
        while (i < length) {
            dst[off + i] = get(index + i);
            i = i + 1;
        }
        return this;
    }

    /**
     * Fills an array from this buffer starting at an absolute index, without moving the position.
     *
     * @param index the first element of this buffer to read
     * @param dst the array to fill
     * @return this buffer
     * @throws IndexOutOfBoundsException if the range falls outside this buffer
     */
    public DoubleBuffer get(int index, double[] dst) {
        return get(index, dst, 0, dst.length);
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
    public DoubleBuffer put(double[] src, int off, int length) {
        checkWritable();
        Buffer.checkBounds(off, length, src.length);
        int to = nextPutIndex(length);
        int i = 0;
        while (i < length) {
            put(to + i, src[off + i]);
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
    public final DoubleBuffer put(double[] src) {
        return put(src, 0, src.length);
    }

    /**
     * Copies part of an array into this buffer at an absolute index, without moving the position.
     *
     * @param index the first element of this buffer to write
     * @param src the array to copy from
     * @param off the first index of {@code src} to read
     * @param length the number of elements to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its container
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(int index, double[] src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        Buffer.checkBounds(off, length, src.length);
        return putArray(index, src, off, length);
    }

    /** The unchecked copy behind the absolute bulk {@code put}. */
    DoubleBuffer putArray(int index, double[] src, int off, int length) {
        int i = 0;
        while (i < length) {
            put(index + i, src[off + i]);
            i = i + 1;
        }
        return this;
    }

    /**
     * Copies a whole array into this buffer at an absolute index, without moving the position.
     *
     * @param index the first element of this buffer to write
     * @param src the array to copy from
     * @return this buffer
     * @throws IndexOutOfBoundsException if the range falls outside this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(int index, double[] src) {
        return put(index, src, 0, src.length);
    }

    /**
     * Drains another buffer into this one: every element remaining in {@code src} is written
     * here, and <em>both</em> positions advance. That both move is the point — this is the
     * hand-off between a buffer that was just filled and one about to be drained.
     *
     * @param src the buffer to copy from
     * @return this buffer
     * @throws BufferOverflowException if {@code src} has more remaining than this buffer
     * @throws IllegalArgumentException if {@code src} is this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(DoubleBuffer src) {
        if (src == this) {
            throw Buffer.createSameBufferException();
        }
        checkWritable();
        int n = src.remaining();
        if (n > remaining()) {
            throw new BufferOverflowException();
        }
        int to = nextPutIndex(n);
        int from = src.nextGetIndex(n);
        int i = 0;
        while (i < n) {
            put(to + i, src.get(from + i));
            i = i + 1;
        }
        return this;
    }

    /**
     * Copies a range of another buffer into this one at an absolute index. Neither position
     * moves; both ranges are absolute.
     *
     * @param index the first element of this buffer to write
     * @param src the buffer to copy from
     * @param off the first element of {@code src} to read
     * @param length the number of elements to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(int index, DoubleBuffer src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        src.checkIndex(off, length);
        putBuffer(index, src, off, length);
        return this;
    }

    /** The unchecked copy behind {@link #put(int, DoubleBuffer, int, int)}. */
    void putBuffer(int index, DoubleBuffer src, int off, int length) {
        int i = 0;
        while (i < length) {
            put(index + i, src.get(off + i));
            i = i + 1;
        }
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
    public abstract DoubleBuffer compact();

    /**
     * Tells whether this buffer's memory lives outside the Java heap.
     *
     * @return always {@code false} — KajiLibrary has no off-heap allocation
     */
    public abstract boolean isDirect();

    /**
     * Tells whether this buffer exposes an accessible backing array.
     *
     * @return {@code true} only for a writable buffer that owns an array; a read-only buffer and
     *         a view over a {@link ByteBuffer} both say no
     */
    public final boolean hasArray() {
        return hb != null && !isReadOnly;
    }

    /**
     * Returns the backing array.
     *
     * @return the array this buffer reads and writes
     * @throws ReadOnlyBufferException if this buffer is read-only
     * @throws UnsupportedOperationException if this buffer is a view and owns no array
     */
    public final double[] array() {
        if (hb == null) {
            throw new UnsupportedOperationException("view buffer has no backing array");
        }
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return hb;
    }

    /**
     * Returns the index in the backing array of this buffer's first element.
     *
     * @return the offset of element zero within the array
     * @throws ReadOnlyBufferException if this buffer is read-only
     * @throws UnsupportedOperationException if this buffer is a view and owns no array
     */
    public final int arrayOffset() {
        if (hb == null) {
            throw new UnsupportedOperationException("view buffer has no backing array");
        }
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return offset;
    }

    /**
     * Sets the position. Declared here only to return the precise type, so that a chain of
     * buffer calls keeps its type.
     *
     * <p>The JDK's version of these seven overrides calls {@code super.position(...)}; ours
     * calls the package-private {@code setPosition} in {@link Buffer} instead, because our javac
     * does not emit {@code invokespecial} for {@code super.method()} yet. Same code, one place.
     *
     * @param newPosition the new position
     * @return this buffer
     */
    public final DoubleBuffer position(int newPosition) {
        setPosition(newPosition);
        return this;
    }

    /**
     * Sets the limit.
     *
     * @param newLimit the new limit
     * @return this buffer
     */
    public final DoubleBuffer limit(int newLimit) {
        setLimit(newLimit);
        return this;
    }

    /**
     * Remembers the current position.
     *
     * @return this buffer
     */
    public final DoubleBuffer mark() {
        setMark();
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     */
    public final DoubleBuffer reset() {
        resetToMark();
        return this;
    }

    /**
     * Resets the indices for writing from scratch.
     *
     * @return this buffer
     */
    public final DoubleBuffer clear() {
        clearIndices();
        return this;
    }

    /**
     * Turns a written buffer into a readable one.
     *
     * @return this buffer
     */
    public final DoubleBuffer flip() {
        flipIndices();
        return this;
    }

    /**
     * Rewinds to position zero, keeping the limit.
     *
     * @return this buffer
     */
    public final DoubleBuffer rewind() {
        rewindIndices();
        return this;
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

    /**
     * Returns the array this buffer's elements live in, or {@code null} for a view over a
     * {@link ByteBuffer}.
     *
     * <p>Not the same question as {@link #array()}: that one is the caller-facing accessor and
     * refuses to answer for a read-only buffer, while this is the implementation's own handle on
     * the storage and always tells the truth.
     *
     * @return the backing array, or {@code null}
     */
    Object base() {
        return hb;
    }

    /**
     * Returns the shift that turns an element index into a byte offset.
     *
     * @return 3, because a double is eight bytes wide
     */
    int scaleShifts() {
        return 3;
    }

    final void checkWritable() {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
    }

    /**
     * Compares this buffer with another for equality of their <em>remaining</em> elements —
     * what is left to read, not the whole array and not the indices.
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is a {@code DoubleBuffer} with the same
     *         remaining contents
     */
    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof DoubleBuffer) {
            DoubleBuffer that = (DoubleBuffer) other;
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
    /**
     * The three-way comparison of two elements.
     *
     * <p>Split out of {@link #compareTo} because a buffer needs a <em>total</em> order over its
     * element type, and {@code <} does not give one here: {@code NaN} compares false against
     * everything, itself included. The order below is the one {@code Float.compare} defines —
     * {@code NaN} sorts above every number and equals itself — so that sorting a buffer of
     * {@code double} terminates and {@code compareTo} stays consistent with itself.
     *
     * @param x the first element
     * @param y the second element
     * @return a negative number, zero or a positive number as {@code x} is less than, equal to
     *         or greater than {@code y}
     */
    private static int compare(double x, double y) {
        if (x < y) {
            return -1;
        }
        if (x > y) {
            return 1;
        }
        if (x == y) {
            return 0;
        }
        if (x != x) {
            if (y != y) {
                return 0;
            }
            return 1;
        }
        return -1;
    }

    public int compareTo(DoubleBuffer that) {
        int n = remaining();
        if (that.remaining() < n) {
            n = that.remaining();
        }
        int i = 0;
        while (i < n) {
            int c = compare(get(position() + i), that.get(that.position() + i));
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return remaining() - that.remaining();
    }

    /**
     * Returns the index of the first element where this buffer and {@code that} disagree,
     * relative to their positions, or {@code -1} if the shared prefix runs to the end of both.
     *
     * <p>When one is a strict prefix of the other the answer is the shorter length: the buffers
     * do differ, and the first place they do is where the shorter one ended.
     *
     * @param that the buffer to compare with
     * @return the relative index of the first mismatch, or {@code -1}
     */
    public int mismatch(DoubleBuffer that) {
        int length = remaining();
        if (that.remaining() < length) {
            length = that.remaining();
        }
        int i = 0;
        while (i < length) {
            if (get(position() + i) != that.get(that.position() + i)) {
                return i;
            }
            i = i + 1;
        }
        if (remaining() != that.remaining()) {
            return length;
        }
        return -1;
    }

    /**
     * Returns a summary of this buffer's indices — not its contents.
     *
     * @return a string of the form {@code DoubleBuffer[pos=.. lim=.. cap=..]}
     */
    public String toString() {
        return "DoubleBuffer[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]";
    }
}

/**
 * The heap implementation of {@link DoubleBuffer}. Top-level and package-private rather than
 * nested — the project's idiom for a helper type.
 *
 * <p>Everything here is either a method the abstract class left open or an array fast path for
 * a bulk operation the abstract class can only express one element at a time (it has to work
 * for views, which have no array).
 */
class HeapDoubleBuffer extends DoubleBuffer {

    HeapDoubleBuffer(double[] hb, int offset, int capacity) {
        super(hb, offset, capacity);
    }

    /**
     * Translates a buffer index into an index in the backing array.
     *
     * <p>The offset exists because a slice shares its parent's array and starts partway into it;
     * every access here goes through this one place so that the arithmetic is written once.
     *
     * @param i the buffer index
     * @return the corresponding index in {@code hb}
     */
    protected int ix(int i) {
        return offset + i;
    }

    public DoubleBuffer slice() {
        HeapDoubleBuffer b = new HeapDoubleBuffer(hb, offset + position(), remaining());
        b.isReadOnly = isReadOnly;
        return b;
    }

    public DoubleBuffer slice(int index, int length) {
        checkIndex(index, length);
        HeapDoubleBuffer b = new HeapDoubleBuffer(hb, offset + index, length);
        b.isReadOnly = isReadOnly;
        return b;
    }

    public DoubleBuffer duplicate() {
        HeapDoubleBuffer b = new HeapDoubleBuffer(hb, offset, capacity());
        b.position(position());
        b.limit(limit());
        b.isReadOnly = isReadOnly;
        return b;
    }

    public DoubleBuffer asReadOnlyBuffer() {
        HeapDoubleBuffer b = (HeapDoubleBuffer) duplicate();
        b.isReadOnly = true;
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
        return isReadOnly;
    }

    public boolean isDirect() {
        return false;
    }

    public double get() {
        return hb[ix(nextGetIndex())];
    }

    public double get(int index) {
        return hb[ix(checkIndex(index))];
    }

    public DoubleBuffer put(double value) {
        checkWritable();
        hb[ix(nextPutIndex())] = value;
        return this;
    }

    public DoubleBuffer put(int index, double value) {
        checkWritable();
        hb[ix(checkIndex(index))] = value;
        return this;
    }

    public DoubleBuffer get(double[] dst, int off, int length) {
        Buffer.checkBounds(off, length, dst.length);
        int from = nextGetIndex(length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + from + i];
            i = i + 1;
        }
        return this;
    }

    public DoubleBuffer get(int index, double[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + index + i];
            i = i + 1;
        }
        return this;
    }

    public DoubleBuffer put(double[] src, int off, int length) {
        checkWritable();
        Buffer.checkBounds(off, length, src.length);
        int to = nextPutIndex(length);
        int i = 0;
        while (i < length) {
            hb[offset + to + i] = src[off + i];
            i = i + 1;
        }
        return this;
    }

    public DoubleBuffer put(int index, double[] src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        Buffer.checkBounds(off, length, src.length);
        int i = 0;
        while (i < length) {
            hb[offset + index + i] = src[off + i];
            i = i + 1;
        }
        return this;
    }

    /**
     * Drains another buffer into this one, copying array to array when the source has one.
     *
     * <p>The abstract class can only do this one element at a time, because it also has to work
     * for a view over a {@link ByteBuffer}, which owns no array. Here both sides usually do.
     *
     * @param src the buffer to copy from
     * @return this buffer
     * @throws BufferOverflowException if {@code src} has more remaining than this buffer
     * @throws IllegalArgumentException if {@code src} is this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(DoubleBuffer src) {
        if (src == this) {
            throw Buffer.createSameBufferException();
        }
        checkWritable();
        int n = src.remaining();
        if (n > remaining()) {
            throw new BufferOverflowException();
        }
        int to = nextPutIndex(n);
        int from = src.nextGetIndex(n);
        int i = 0;
        if (src.hb != null) {
            while (i < n) {
                hb[offset + to + i] = src.hb[src.offset + from + i];
                i = i + 1;
            }
        } else {
            while (i < n) {
                hb[offset + to + i] = src.get(from + i);
                i = i + 1;
            }
        }
        return this;
    }

    /**
     * Copies a range of another buffer into this one at an absolute index, array to array when
     * the source has one. Neither position moves.
     *
     * @param index the first element of this buffer to write
     * @param src the buffer to copy from
     * @param off the first element of {@code src} to read
     * @param length the number of elements to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public DoubleBuffer put(int index, DoubleBuffer src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        src.checkIndex(off, length);
        if (src.hb == null) {
            putBuffer(index, src, off, length);
            return this;
        }
        int i = 0;
        while (i < length) {
            hb[offset + index + i] = src.hb[src.offset + off + i];
            i = i + 1;
        }
        return this;
    }

    public DoubleBuffer compact() {
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

    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}
