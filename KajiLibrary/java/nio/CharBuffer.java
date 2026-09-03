package java.nio;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * A buffer of char values.
 *
 * <p>Every typed buffer is the same four indices as {@link Buffer} over an array of one element
 * type; what changes is only the width of an element, which is why these six classes are
 * near-identical by construction rather than by accident. In the JDK they are literally generated
 * from a single template, and so are these.
 *
 * <p>The interesting asymmetry is with {@link ByteBuffer}: bytes are what actually travel, so
 * only that class can reinterpret its contents at other widths. A {@code CharBuffer} is either
 * allocated on its own — and then it owns a {@code char[]} — or obtained as a <em>view</em> over
 * some bytes via {@link ByteBuffer#asCharBuffer()}, and then it owns nothing and every access is
 * decoded from the bytes underneath. That second case is why {@link #hb} may be {@code null} and
 * why the bulk operations below are written in terms of {@link #get(int)} and
 * {@link #put(int, char)} rather than reaching into the array: a view has no array to reach into.
 *
 * <p>CharBuffer is the one typed buffer that is also <em>text</em>: it implements
 * {@link CharSequence}, so anything that takes a char sequence takes a slice of a buffer without
 * copying it, and {@link Appendable}, so anything that writes characters can write into one. That
 * is why {@link #toString()} here returns the buffer's <em>contents</em> and not a summary of its
 * indices, unlike every other buffer in this package - the {@code CharSequence} contract requires
 * it, and a {@code CharBuffer} that printed "pos=.. lim=.." would silently corrupt every string
 * concatenation it took part in.
 *
 * <p><strong>Omitted from this implementation</strong> (the API-shape gate requires a subset, not
 * equality): everything that needs {@code Unsafe}, {@code MemorySegment} or a scoped session -
 * {@code heapSegment(...)} and the {@code MemorySegment} constructors - because there is no
 * off-heap memory here at all; and the {@code java.lang.Readable} interface, which KajiLibrary
 * does not declare - {@link #read(CharBuffer)} is here with the right signature, but the class
 * cannot name the interface it belongs to.
 */
public abstract class CharBuffer extends Buffer
        implements Comparable<CharBuffer>, Appendable, CharSequence, Readable {

    /**
     * The backing array, or {@code null} for a view over a {@link ByteBuffer}. Every accessor
     * that touches it has to ask first; that is the whole cost of supporting views.
     */
    final char[] hb;
    final int offset;
    boolean isReadOnly;

    CharBuffer(char[] hb, int offset, int capacity) {
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
    public static CharBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw Buffer.createCapacityException(capacity);
        }
        return new HeapCharBuffer(new char[capacity], 0, capacity);
    }

    /**
     * Wraps an array in a buffer. The array is <em>not</em> copied: writes through the buffer are
     * visible in the array and vice versa.
     *
     * @param array the array to wrap
     * @return a buffer over {@code array}
     */
    public static CharBuffer wrap(char[] array) {
        return new HeapCharBuffer(array, 0, array.length);
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
    public static CharBuffer wrap(char[] array, int offset, int length) {
        HeapCharBuffer b = new HeapCharBuffer(array, 0, array.length);
        b.position(offset);
        b.limit(offset + length);
        return b;
    }

    /**
     * Wraps a range of a character sequence in a read-only buffer.
     *
     * <p>Read-only because the sequence may be a {@code String}, which is immutable - the buffer
     * cannot offer a write it could not perform. Nothing is copied: this is how a parser reads a
     * {@code String} through the buffer API without paying for a {@code char[]}.
     *
     * @param csq the sequence to wrap
     * @param start the index of the first character
     * @param end the index just past the last character
     * @return a read-only buffer over {@code csq}
     * @throws IndexOutOfBoundsException if the range falls outside {@code csq}
     */
    public static CharBuffer wrap(CharSequence csq, int start, int end) {
        if (start < 0 || end < start || end > csq.length()) {
            throw new IndexOutOfBoundsException("range out of bounds for the sequence");
        }
        CharSequenceBuffer b = new CharSequenceBuffer(csq, 0, csq.length());
        b.limit(end);
        b.position(start);
        return b;
    }

    /**
     * Wraps a whole character sequence in a read-only buffer.
     *
     * @param csq the sequence to wrap
     * @return a read-only buffer over {@code csq}
     */
    public static CharBuffer wrap(CharSequence csq) {
        return wrap(csq, 0, csq.length());
    }

    /**
     * Reads characters out of this buffer into another one - the {@code java.lang.Readable}
     * contract, which is what makes a {@code CharBuffer} usable as a source for the same code
     * that reads from a {@code Reader}.
     *
     * <p>KajiLibrary has no {@code java.lang.Readable} interface, so this method is present with
     * the right signature but the class cannot declare that it implements it.
     *
     * @param target the buffer to read into
     * @return the number of characters transferred, or {@code -1} if this buffer is exhausted
     * @throws IOException never here; declared to match the contract
     * @throws IllegalArgumentException if {@code target} is this buffer
     * @throws ReadOnlyBufferException if {@code target} is read-only
     */
    public int read(CharBuffer target) throws IOException {
        if (target == this) {
            throw Buffer.createSameBufferException();
        }
        target.checkWritable();
        int n = remaining();
        if (n <= 0) {
            return -1;
        }
        if (target.remaining() < n) {
            n = target.remaining();
        }
        int from = nextGetIndex(n);
        int to = target.nextPutIndex(n);
        int i = 0;
        while (i < n) {
            target.put(to + i, get(from + i));
            i = i + 1;
        }
        return n;
    }

    /**
     * Creates a buffer over this buffer's remaining elements, sharing the backing memory.
     *
     * @return the new buffer, whose position is zero and whose capacity is the remaining count
     */
    public abstract CharBuffer slice();

    /**
     * Creates a buffer over the given range of this one, sharing the backing memory.
     *
     * @param index the first element of the slice
     * @param length the number of elements in the slice
     * @return the new buffer
     */
    public abstract CharBuffer slice(int index, int length);

    /**
     * Creates a buffer sharing this one's backing memory but with its own position, limit and
     * mark — two independent cursors over one piece of memory.
     *
     * @return the new buffer
     */
    public abstract CharBuffer duplicate();

    /**
     * Creates a read-only view of this buffer, sharing its contents.
     *
     * @return the new buffer, whose writes throw {@link ReadOnlyBufferException}
     */
    public abstract CharBuffer asReadOnlyBuffer();

    /**
     * Reads the element at the current position and advances the position.
     *
     * @return the element read
     * @throws BufferUnderflowException if the position is at the limit
     */
    public abstract char get();

    /**
     * Reads the element at the given index without moving the position.
     *
     * @param index the index to read
     * @return the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     */
    public abstract char get(int index);

    /**
     * Writes the value at the current position and advances the position.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract CharBuffer put(char value);

    /**
     * Writes the value at the given index without moving the position.
     *
     * @param index the index to write
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if {@code index} is outside the buffer's limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract CharBuffer put(int index, char value);

    /**
     * Copies elements from this buffer into part of an array, advancing the position.
     *
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of elements to copy
     * @return this buffer
     * @throws BufferUnderflowException if fewer than {@code length} elements remain
     */
    public CharBuffer get(char[] dst, int off, int length) {
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
    public CharBuffer get(char[] dst) {
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
    public CharBuffer get(int index, char[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        return getArray(index, dst, off, length);
    }

    /** The unchecked copy behind the absolute bulk {@code get}. */
    private CharBuffer getArray(int index, char[] dst, int off, int length) {
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
    public CharBuffer get(int index, char[] dst) {
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
    public CharBuffer put(char[] src, int off, int length) {
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
    public final CharBuffer put(char[] src) {
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
    public CharBuffer put(int index, char[] src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        Buffer.checkBounds(off, length, src.length);
        return putArray(index, src, off, length);
    }

    /** The unchecked copy behind the absolute bulk {@code put}. */
    CharBuffer putArray(int index, char[] src, int off, int length) {
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
    public CharBuffer put(int index, char[] src) {
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
    public CharBuffer put(CharBuffer src) {
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
    public CharBuffer put(int index, CharBuffer src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        src.checkIndex(off, length);
        putBuffer(index, src, off, length);
        return this;
    }

    /** The unchecked copy behind {@link #put(int, CharBuffer, int, int)}. */
    void putBuffer(int index, CharBuffer src, int off, int length) {
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
    public abstract CharBuffer compact();

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
    public final char[] array() {
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
    public final CharBuffer position(int newPosition) {
        setPosition(newPosition);
        return this;
    }

    /**
     * Sets the limit.
     *
     * @param newLimit the new limit
     * @return this buffer
     */
    public final CharBuffer limit(int newLimit) {
        setLimit(newLimit);
        return this;
    }

    /**
     * Remembers the current position.
     *
     * @return this buffer
     */
    public final CharBuffer mark() {
        setMark();
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     */
    public final CharBuffer reset() {
        resetToMark();
        return this;
    }

    /**
     * Resets the indices for writing from scratch.
     *
     * @return this buffer
     */
    public final CharBuffer clear() {
        clearIndices();
        return this;
    }

    /**
     * Turns a written buffer into a readable one.
     *
     * @return this buffer
     */
    public final CharBuffer flip() {
        flipIndices();
        return this;
    }

    /**
     * Rewinds to position zero, keeping the limit.
     *
     * @return this buffer
     */
    public final CharBuffer rewind() {
        rewindIndices();
        return this;
    }

    /**
     * Copies a range of a string into this buffer, advancing the position.
     *
     * @param src the string to copy from
     * @param start the index of the first character
     * @param end the index just past the last character
     * @return this buffer
     * @throws BufferOverflowException if fewer than {@code end - start} characters of room remain
     * @throws IndexOutOfBoundsException if the range falls outside {@code src}
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public CharBuffer put(String src, int start, int end) {
        checkWritable();
        if (start < 0 || end < start || end > src.length()) {
            throw new IndexOutOfBoundsException("range out of bounds for the string");
        }
        int n = end - start;
        int to = nextPutIndex(n);
        int i = 0;
        while (i < n) {
            put(to + i, src.charAt(start + i));
            i = i + 1;
        }
        return this;
    }

    /**
     * Copies a whole string into this buffer, advancing the position.
     *
     * @param src the string to copy
     * @return this buffer
     * @throws BufferOverflowException if fewer than {@code src.length()} characters of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public final CharBuffer put(String src) {
        return put(src, 0, src.length());
    }

    /**
     * Copies characters into an array, with the source indices measured from the position - the
     * {@code String.getChars} convention rather than the buffer one.
     *
     * @param srcBegin the first character to copy, relative to the position
     * @param srcEnd just past the last character to copy, relative to the position
     * @param dst the array to copy into
     * @param dstBegin the first index of {@code dst} to write
     * @throws IndexOutOfBoundsException if either range falls outside its container
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcEnd < srcBegin || srcEnd > length()) {
            throw new IndexOutOfBoundsException("range out of bounds for this buffer");
        }
        int n = srcEnd - srcBegin;
        Buffer.checkBounds(dstBegin, n, dst.length);
        int pos = position();
        int i = 0;
        while (i < n) {
            dst[dstBegin + i] = get(pos + srcBegin + i);
            i = i + 1;
        }
    }

    /**
     * Returns the number of characters between the position and the limit - the
     * {@link CharSequence} view of {@link #remaining()}.
     *
     * @return the number of characters remaining
     */
    public final int length() {
        return remaining();
    }

    /**
     * Tells whether this sequence is empty.
     *
     * @return {@code true} if nothing remains between the position and the limit
     */
    public final boolean isEmpty() {
        return remaining() == 0;
    }

    /**
     * Returns the character at {@code index} <em>counted from the position</em>, which is what
     * {@link CharSequence} means by an index and is not what {@link #get(int)} means by one.
     *
     * @param index the index relative to the position
     * @return the character there
     * @throws IndexOutOfBoundsException if {@code index} is outside the remaining characters
     */
    public final char charAt(int index) {
        if (index < 0 || index >= remaining()) {
            throw new IndexOutOfBoundsException("index out of bounds for the sequence");
        }
        return get(position() + index);
    }

    /**
     * Returns a buffer over part of the remaining characters, sharing the memory.
     *
     * <p>Unlike {@link #slice(int, int)} the capacity is <em>not</em> narrowed: the result is
     * this buffer with different indices, so a caller can still widen it back out.
     *
     * @param start the first character, relative to the position
     * @param end just past the last character, relative to the position
     * @return the new buffer
     * @throws IndexOutOfBoundsException if the range falls outside the remaining characters
     */
    public abstract CharBuffer subSequence(int start, int end);

    /**
     * The body shared by every {@code subSequence} below; see {@link #subSequence(int, int)}.
     *
     * <p>It cannot simply live in {@code subSequence} itself: the JDK declares that method
     * abstract, and matching that is the point.
     */
    final CharBuffer subSequenceImpl(int start, int end) {
        if (start < 0 || end < start || end > remaining()) {
            throw new IndexOutOfBoundsException("range out of bounds for the sequence");
        }
        int pos = position();
        CharBuffer b = duplicate();
        b.limit(pos + end);
        b.position(pos + start);
        return b;
    }

    /**
     * Appends a character sequence, advancing the position - the {@link Appendable} contract.
     *
     * @param csq the sequence to append, or {@code null} for the four characters {@code null}
     * @return this buffer
     * @throws BufferOverflowException if there is not enough room
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public CharBuffer append(CharSequence csq) {
        if (csq == null) {
            return put("null");
        }
        return put(csq.toString());
    }

    /**
     * Appends part of a character sequence, advancing the position.
     *
     * @param csq the sequence to append, or {@code null} for the four characters {@code null}
     * @param start the index of the first character
     * @param end the index just past the last character
     * @return this buffer
     * @throws BufferOverflowException if there is not enough room
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public CharBuffer append(CharSequence csq, int start, int end) {
        CharSequence cs = csq;
        if (cs == null) {
            cs = "null";
        }
        return put(cs.subSequence(start, end).toString());
    }

    /**
     * Appends one character, advancing the position.
     *
     * @param c the character to append
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public CharBuffer append(char c) {
        return put(c);
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
     * Returns the byte order of the region this buffer's characters live in, or {@code null}
     * when they do not live in bytes at all.
     *
     * <p>Not the same question as {@link #order()}, which always answers. A buffer wrapping a
     * {@link CharSequence} has characters but no bytes behind them — it decodes from whatever the
     * sequence is — and answering {@code null} is how a caller that wanted to copy the region as
     * raw bytes learns it has to go character by character instead.
     *
     * @return the byte order of the underlying bytes, or {@code null} if there are none
     */
    abstract ByteOrder charRegionOrder();

    /**
     * Reads the character at an absolute index with no bounds check.
     *
     * <p>The checked accessors validate and then land here. It exists as a separate method
     * because the bulk operations validate a whole range once and then must not pay for a
     * per-character check they have already made.
     *
     * @param index the absolute index, already known to be in bounds
     * @return the character at {@code index}
     */
    abstract char getUnchecked(int index);

    /**
     * Tells whether this buffer's characters occupy addressable storage — an array or a region of
     * bytes — rather than being produced on demand.
     *
     * @return {@code true} unless this buffer is a view over a {@link CharSequence}
     */
    boolean isAddressable() {
        return true;
    }

    /**
     * Returns the remaining characters as a stream of their integer values.
     *
     * <p>The elements are {@code int} rather than {@code char} because that is the only primitive
     * stream the language offers for them; the values are the char values, zero-extended, and the
     * buffer's position does not move.
     *
     * @return a stream over the characters between the position and the limit
     */
    public IntStream chars() {
        int n = remaining();
        int[] values = new int[n];
        int i = 0;
        while (i < n) {
            values[i] = (int) get(position() + i);
            i = i + 1;
        }
        return IntStream.of(values);
    }

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
     * @return 1, because a char is two bytes wide
     */
    int scaleShifts() {
        return 1;
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
     * @return {@code true} if {@code other} is a {@code CharBuffer} with the same
     *         remaining contents
     */
    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof CharBuffer) {
            CharBuffer that = (CharBuffer) other;
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
     * element type, and for the floating-point buffers {@code <} does not give one.
     *
     * @param x the first element
     * @param y the second element
     * @return a negative number, zero or a positive number as {@code x} is less than, equal to
     *         or greater than {@code y}
     */
    private static int compare(char x, char y) {
        if (x < y) {
            return -1;
        }
        if (x > y) {
            return 1;
        }
        return 0;
    }

    public int compareTo(CharBuffer that) {
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
    public int mismatch(CharBuffer that) {
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
     * Returns the remaining characters as a string.
     *
     * <p>The odd one out in this package: every other buffer's {@code toString} is a summary of
     * the indices. Here the {@link CharSequence} contract wins - see the class comment.
     *
     * @return the characters between the position and the limit
     */
    public String toString() {
        return toString(position(), limit());
    }

    /**
     * Renders the characters in {@code [start, end)}, in absolute indices.
     *
     * <p>Abstract because a heap buffer, a view over bytes and a wrapped {@code CharSequence}
     * each have a cheaper way to do it than going through {@code get} one character at a time.
     *
     * @param start the first character
     * @param end just past the last character
     * @return the characters as a string
     */
    abstract String toString(int start, int end);
}

/**
 * The heap implementation of {@link CharBuffer}. Top-level and package-private rather than
 * nested — the project's idiom for a helper type.
 *
 * <p>Everything here is either a method the abstract class left open or an array fast path for
 * a bulk operation the abstract class can only express one element at a time (it has to work
 * for views, which have no array).
 */
class HeapCharBuffer extends CharBuffer {

    /**
     * El constructor **completo**: el arreglo, los cuatro indices, el desplazamiento, y el segmento
     * de memoria del que este buffer es una vista.
     *
     * <p>Existe porque el JDK lo declara, y estuvo afuera hasta ahora por una razon concreta: nombra
     * `java.lang.foreign.MemorySegment`, que no existia en esta biblioteca. Ahora existe.
     *
     * <p><strong>El segmento se acepta y no se guarda</strong>, y conviene decir por que eso no
     * pierde nada aca. En el JDK ese campo es lo que hace que `MemorySegment.ofBuffer(buffer)`
     * devuelva **el mismo** segmento del que el buffer salio. Esta biblioteca lo reconstruye desde el
     * arreglo de respaldo, asi que `ofBuffer` sigue dando una vista correcta de los mismos bytes; lo
     * unico que se pierde es la **identidad** del segmento, que ningun metodo publico expone.
     *
     * <p>Los indices se fijan en el orden que el contrato de `Buffer` obliga --limite antes que
     * posicion-- porque una posicion no puede pasar del limite, y hacerlo al reves fallaria fijando
     * un estado que despues iba a ser valido.
     *
     * @param mark la marca, o negativo si no hay
     */
    protected HeapCharBuffer(char[] buf, int mark, int pos, int lim, int cap, int off,
            java.lang.foreign.MemorySegment segment) {
        super(buf, off, cap);
        this.limit(lim);
        if (mark >= 0) {
            this.position(mark);
            this.mark();
        }
        this.position(pos);
    }

    HeapCharBuffer(char[] hb, int offset, int capacity) {
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

    public CharBuffer slice() {
        HeapCharBuffer b = new HeapCharBuffer(hb, offset + position(), remaining());
        b.isReadOnly = isReadOnly;
        return b;
    }

    public CharBuffer slice(int index, int length) {
        checkIndex(index, length);
        HeapCharBuffer b = new HeapCharBuffer(hb, offset + index, length);
        b.isReadOnly = isReadOnly;
        return b;
    }

    public CharBuffer duplicate() {
        HeapCharBuffer b = new HeapCharBuffer(hb, offset, capacity());
        b.position(position());
        b.limit(limit());
        b.isReadOnly = isReadOnly;
        return b;
    }

    public CharBuffer asReadOnlyBuffer() {
        HeapCharBuffer b = (HeapCharBuffer) duplicate();
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

    public char get() {
        return hb[ix(nextGetIndex())];
    }

    public char get(int index) {
        return hb[ix(checkIndex(index))];
    }

    char getUnchecked(int index) {
        return hb[ix(index)];
    }

    ByteOrder charRegionOrder() {
        return order();
    }

    public CharBuffer put(char value) {
        checkWritable();
        hb[ix(nextPutIndex())] = value;
        return this;
    }

    public CharBuffer put(int index, char value) {
        checkWritable();
        hb[ix(checkIndex(index))] = value;
        return this;
    }

    public CharBuffer get(char[] dst, int off, int length) {
        Buffer.checkBounds(off, length, dst.length);
        int from = nextGetIndex(length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + from + i];
            i = i + 1;
        }
        return this;
    }

    public CharBuffer get(int index, char[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + index + i];
            i = i + 1;
        }
        return this;
    }

    public CharBuffer put(char[] src, int off, int length) {
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

    public CharBuffer put(int index, char[] src, int off, int length) {
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
    public CharBuffer put(CharBuffer src) {
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
    public CharBuffer put(int index, CharBuffer src, int off, int length) {
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

    public CharBuffer compact() {
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
     * Copies a range of a character sequence straight into the backing array.
     *
     * <p>This is why the heap buffer overrides {@code append} and {@code put(String, int, int)}
     * at all instead of inheriting them. The versions in {@link CharBuffer} have to work for a
     * view that owns no array, so they write one character at a time through the abstract
     * {@link CharBuffer#put(int, char)}; here the destination <em>is</em> an array, so the loop
     * is a plain array store.
     *
     * <p>The sequence is turned into a {@code String} first, which the JDK does not do. Our VM
     * cannot dispatch an interface call to a {@code String} receiver — {@code charAt} and
     * {@code length} reached through {@code CharSequence} abort the interpreter — and a
     * {@code String} is the sequence practically every caller passes. {@code toString()} does
     * work, and returns the receiver itself when it is already a {@code String}, so the common
     * case costs nothing and the uncommon one costs the copy the JDK avoids.
     *
     * @param csq the sequence to copy from
     * @param start the index of the first character
     * @param end the index just past the last character
     * @return this buffer
     */
    private CharBuffer appendChars(CharSequence csq, int start, int end) {
        checkWritable();
        String s = csq.toString();
        int n = end - start;
        int to = nextPutIndex(n);
        int i = 0;
        while (i < n) {
            hb[ix(to + i)] = s.charAt(start + i);
            i = i + 1;
        }
        return this;
    }

    public CharBuffer append(CharSequence csq) {
        String s = "null";
        if (csq != null) {
            s = csq.toString();
        }
        return appendChars(s, 0, s.length());
    }

    public CharBuffer append(CharSequence csq, int start, int end) {
        String s = "null";
        if (csq != null) {
            s = csq.toString();
        }
        if (start < 0 || end < start || end > s.length()) {
            throw new IndexOutOfBoundsException("range out of bounds for the sequence");
        }
        return appendChars(s, start, end);
    }

    public CharBuffer put(String src, int start, int end) {
        if (start < 0 || end < start || end > src.length()) {
            throw new IndexOutOfBoundsException("range out of bounds for the string");
        }
        return appendChars(src, start, end);
    }

    public CharBuffer subSequence(int start, int end) {
        return subSequenceImpl(start, end);
    }

    String toString(int start, int end) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < end) {
            sb.append(hb[offset + i]);
            i = i + 1;
        }
        return sb.toString();
    }

    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}

/**
 * A read-only {@link CharBuffer} whose characters come from an arbitrary {@link CharSequence}.
 * This is what {@link CharBuffer#wrap(CharSequence)} returns.
 *
 * <p>It is what makes the buffer API usable over a {@code String} without first copying it into a
 * {@code char[]}: a parser written against {@code CharBuffer} can be pointed at a literal, a
 * {@code StringBuilder}, or a slice of another buffer, and the only thing it gives up is writing.
 *
 * <p>Read-only is not a policy choice here, it is arithmetic: a {@code CharSequence} has no
 * setter. Offering {@code put} would mean either failing at run time anyway or silently keeping a
 * shadow copy, and the second one would break the aliasing the class exists for.
 *
 * <p>Named unlike the JDK's internal {@code StringCharBuffer} on purpose, so that nothing here
 * can be mistaken for a copy of {@code java.base}'s internals.
 */
class CharSequenceBuffer extends CharBuffer {

    /** The sequence the characters come from - shared, never copied. */
    private final CharSequence csq;

    /** The index in {@link #csq} of this buffer's character zero. */
    private final int seqOffset;

    CharSequenceBuffer(CharSequence csq, int seqOffset, int capacity) {
        super(null, 0, capacity);
        this.csq = csq;
        this.seqOffset = seqOffset;
        this.isReadOnly = true;
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean isDirect() {
        return false;
    }

    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    public char get() {
        return csq.charAt(seqOffset + nextGetIndex());
    }

    public char get(int index) {
        return csq.charAt(seqOffset + checkIndex(index));
    }

    char getUnchecked(int index) {
        return csq.charAt(seqOffset + index);
    }

    /**
     * @return {@code null}; the characters are produced by a {@link CharSequence} and there are
     *         no bytes under them to have an order
     */
    ByteOrder charRegionOrder() {
        return null;
    }

    /**
     * @return {@code false}; there is no array and no byte region, only a sequence answering one
     *         character at a time
     */
    boolean isAddressable() {
        return false;
    }

    public CharBuffer put(char value) {
        throw new ReadOnlyBufferException();
    }

    public CharBuffer put(int index, char value) {
        throw new ReadOnlyBufferException();
    }

    public CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    public CharBuffer slice() {
        return new CharSequenceBuffer(csq, seqOffset + position(), remaining());
    }

    public CharBuffer slice(int index, int length) {
        checkIndex(index, length);
        return new CharSequenceBuffer(csq, seqOffset + index, length);
    }

    public CharBuffer duplicate() {
        CharSequenceBuffer b = new CharSequenceBuffer(csq, seqOffset, capacity());
        b.limit(limit());
        b.position(position());
        return b;
    }

    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    public CharBuffer subSequence(int start, int end) {
        return subSequenceImpl(start, end);
    }

    String toString(int start, int end) {
        return csq.subSequence(seqOffset + start, seqOffset + end).toString();
    }
}
