package java.nio;

/**
 * A buffer of bytes, and the only one whose contents can be reinterpreted at other widths.
 *
 * <p>That asymmetry is the design of the whole package: bytes are what actually travel, and every
 * other buffer type is a <em>view</em> over some bytes — which is why the multi-byte accessors and
 * {@link ByteOrder} live here and nowhere else.
 *
 * <p>Two access styles coexist deliberately. <strong>Relative</strong> ({@link #get()},
 * {@link #put(byte)}) moves the position and is what a stream-like reader wants;
 * <strong>absolute</strong> ({@link #get(int)}, {@link #put(int, byte)}) leaves it alone, which is
 * what makes one buffer usable from more than one place at a time. Confusing the two is the
 * classic source of a corrupted parse.
 *
 * <p>Every buffer created here is a <em>heap</em> buffer over a {@code byte[]};
 * {@link #allocateDirect(int)} says so rather than pretending otherwise.
 *
 * <p><strong>Omitted from this implementation</strong> (the API-shape gate requires a subset, not
 * equality): {@code getFloat}/{@code putFloat}/{@code getDouble}/{@code putDouble}, which need
 * bit-conversion intrinsics KajiLibrary does not have — only {@code Double.doubleToLongBits}
 * exists, and not its inverse, so they would be half-implementable, which is worse than absent;
 * the six view factories ({@code asIntBuffer} and friends); and {@code mismatch},
 * {@code alignmentOffset} and {@code alignedSlice}.
 */
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {

    final byte[] hb;
    final int offset;
    boolean readOnly;
    boolean bigEndian;

    ByteBuffer(byte[] hb, int offset, int capacity) {
        super(capacity);
        this.hb = hb;
        this.offset = offset;
        // The JDK's default is BIG_ENDIAN regardless of the machine — a deliberate choice, since a
        // buffer usually parses a FORMAT, and formats are mostly big-endian on the wire.
        this.bigEndian = true;
    }

    /**
     * Allocates a buffer with the given capacity, backed by a fresh array.
     *
     * @param capacity the number of bytes the buffer holds
     * @return the new buffer, positioned at zero with the limit at the capacity
     * @throws IllegalArgumentException if {@code capacity} is negative
     */
    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        return new HeapByteBuffer(new byte[capacity], 0, capacity);
    }

    /**
     * Allocates a buffer outside the Java heap, so that I/O can hand the memory to the operating
     * system without copying.
     *
     * <p>KajiLibrary has no off-heap allocation, so this returns an ordinary heap buffer: the same
     * behaviour, none of the reason the method exists. {@link #isDirect()} reports the truth.
     *
     * @param capacity the number of bytes the buffer holds
     * @return the new buffer
     * @throws IllegalArgumentException if {@code capacity} is negative
     */
    public static ByteBuffer allocateDirect(int capacity) {
        return allocate(capacity);
    }

    /**
     * Wraps part of an array, positioned at {@code offset} with the limit just past the length.
     * The array is not copied.
     *
     * @param array the array to wrap
     * @param offset the initial position
     * @param length the number of bytes between the position and the limit
     * @return a buffer over {@code array}
     */
    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        HeapByteBuffer b = new HeapByteBuffer(array, 0, array.length);
        b.position(offset);
        b.limit(offset + length);
        return b;
    }

    /**
     * Wraps an array. The array is <em>not</em> copied: writes through the buffer are visible in
     * the array and vice versa.
     *
     * @param array the array to wrap
     * @return a buffer over {@code array}
     */
    public static ByteBuffer wrap(byte[] array) {
        return new HeapByteBuffer(array, 0, array.length);
    }

    /**
     * Creates a buffer over this buffer's remaining bytes, sharing the backing array.
     *
     * @return the new buffer
     */
    public abstract ByteBuffer slice();

    /**
     * Creates a buffer over the given range of this one, sharing the backing array.
     *
     * @param index the first byte of the slice
     * @param length the number of bytes in the slice
     * @return the new buffer
     */
    public abstract ByteBuffer slice(int index, int length);

    /**
     * Creates a buffer sharing this one's array but with its own position, limit and mark.
     *
     * @return the new buffer
     */
    public abstract ByteBuffer duplicate();

    /**
     * Creates a read-only view sharing this buffer's contents.
     *
     * @return the new buffer, whose writes throw {@link ReadOnlyBufferException}
     */
    public abstract ByteBuffer asReadOnlyBuffer();

    /**
     * Reads the byte at the current position and advances it.
     *
     * @return the byte read
     * @throws BufferUnderflowException if the position is at the limit
     */
    public abstract byte get();

    /**
     * Reads the byte at the given index without moving the position.
     *
     * @param index the index to read
     * @return the byte at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the limit
     */
    public abstract byte get(int index);

    /**
     * Writes a byte at the current position and advances it.
     *
     * @param b the byte to write
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer put(byte b);

    /**
     * Writes a byte at the given index without moving the position.
     *
     * @param index the index to write
     * @param b the byte to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if {@code index} is outside the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer put(int index, byte b);

    /**
     * Copies bytes from this buffer into part of an array, advancing the position.
     *
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of bytes to copy
     * @return this buffer
     * @throws BufferUnderflowException if fewer than {@code length} bytes remain
     */
    public ByteBuffer get(byte[] dst, int off, int length) {
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
     * @throws BufferUnderflowException if fewer than {@code dst.length} bytes remain
     */
    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Copies part of an array into this buffer, advancing the position.
     *
     * @param src the array to copy from
     * @param off the first index of {@code src} to read
     * @param length the number of bytes to copy
     * @return this buffer
     * @throws BufferOverflowException if fewer than {@code length} bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(byte[] src, int off, int length) {
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
     * @throws BufferOverflowException if fewer than {@code src.length} bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * Slides the unread bytes to the front and positions after them.
     *
     * <p>This is the operation for "I consumed part of this, now refill the rest", and it is what
     * makes a fixed buffer usable as a sliding window over an unbounded stream.
     *
     * @return this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer compact() {
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
     * @return always {@code false} — see {@link #allocateDirect(int)}
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
        // The cast is required: the compiler rejects a `byte[]` where an `Object` is expected in a
        // return ("tipo de retorno incompatible"), although every array IS an Object (JLS 4.3.1).
        return (Object) hb;
    }

    /**
     * Returns the index in the backing array of this buffer's first byte.
     *
     * @return the offset of byte zero within the array
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public int arrayOffset() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
        return offset;
    }

    /**
     * Returns the byte order used by the multi-byte accessors.
     *
     * @return {@link ByteOrder#BIG_ENDIAN} unless changed; that is the JDK's default too,
     *         regardless of the machine, because a buffer usually parses a format
     */
    public final ByteOrder order() {
        ByteOrder o = ByteOrder.LITTLE_ENDIAN;
        if (bigEndian) {
            o = ByteOrder.BIG_ENDIAN;
        }
        return o;
    }

    /**
     * Sets the byte order for every multi-byte accessor from here on.
     *
     * <p>The order is buffer <em>state</em>, not an argument, which is what lets a parser set it
     * once and stop thinking about it.
     *
     * @param bo the order to use
     * @return this buffer
     */
    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = bo == ByteOrder.BIG_ENDIAN;
        return this;
    }

    /**
     * Reads two bytes as a char at the current position, advancing it.
     *
     * @return the char read
     * @throws BufferUnderflowException if fewer than two bytes remain
     */
    public char getChar() {
        return (char) readBits(nextGetIndex(2), 2);
    }

    /**
     * Reads two bytes as a char at the given index.
     *
     * @param index the index of the first byte
     * @return the char read
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     */
    public char getChar(int index) {
        return (char) readBits(checkIndex(index, 2), 2);
    }

    /**
     * Writes a char as two bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than two bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putChar(char value) {
        writeBits(nextPutIndex(2), 2, (long) value);
        return this;
    }

    /**
     * Writes a char as two bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putChar(int index, char value) {
        writeBits(checkIndex(index, 2), 2, (long) value);
        return this;
    }

    /**
     * Reads two bytes as a short at the current position, advancing it.
     *
     * @return the short read
     * @throws BufferUnderflowException if fewer than two bytes remain
     */
    public short getShort() {
        return (short) readBits(nextGetIndex(2), 2);
    }

    /**
     * Reads two bytes as a short at the given index.
     *
     * @param index the index of the first byte
     * @return the short read
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     */
    public short getShort(int index) {
        return (short) readBits(checkIndex(index, 2), 2);
    }

    /**
     * Writes a short as two bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than two bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putShort(short value) {
        writeBits(nextPutIndex(2), 2, (long) value);
        return this;
    }

    /**
     * Writes a short as two bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putShort(int index, short value) {
        writeBits(checkIndex(index, 2), 2, (long) value);
        return this;
    }

    /**
     * Reads four bytes as an int at the current position, advancing it.
     *
     * @return the int read
     * @throws BufferUnderflowException if fewer than four bytes remain
     */
    public int getInt() {
        return (int) readBits(nextGetIndex(4), 4);
    }

    /**
     * Reads four bytes as an int at the given index.
     *
     * @param index the index of the first byte
     * @return the int read
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     */
    public int getInt(int index) {
        return (int) readBits(checkIndex(index, 4), 4);
    }

    /**
     * Writes an int as four bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than four bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putInt(int value) {
        writeBits(nextPutIndex(4), 4, (long) value);
        return this;
    }

    /**
     * Writes an int as four bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putInt(int index, int value) {
        writeBits(checkIndex(index, 4), 4, (long) value);
        return this;
    }

    /**
     * Reads eight bytes as a long at the current position, advancing it.
     *
     * @return the long read
     * @throws BufferUnderflowException if fewer than eight bytes remain
     */
    public long getLong() {
        return readBits(nextGetIndex(8), 8);
    }

    /**
     * Reads eight bytes as a long at the given index.
     *
     * @param index the index of the first byte
     * @return the long read
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     */
    public long getLong(int index) {
        return readBits(checkIndex(index, 8), 8);
    }

    /**
     * Writes a long as eight bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than eight bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putLong(long value) {
        writeBits(nextPutIndex(8), 8, value);
        return this;
    }

    /**
     * Writes a long as eight bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer putLong(int index, long value) {
        writeBits(checkIndex(index, 8), 8, value);
        return this;
    }

    // The one place the byte order actually matters: the same bytes, assembled in one direction
    // or the other.
    private long readBits(int at, int count) {
        long value = 0;
        int i = 0;
        while (i < count) {
            int b = hb[offset + at + i] & 0xff;
            if (bigEndian) {
                value = (value << 8) | (long) b;
            } else {
                value = value | ((long) b << (8 * i));
            }
            i = i + 1;
        }
        return value;
    }

    private void writeBits(int at, int count, long value) {
        checkWritable();
        int i = 0;
        while (i < count) {
            int shift = 8 * i;
            if (bigEndian) {
                shift = 8 * (count - 1 - i);
            }
            hb[offset + at + i] = (byte) ((value >> shift) & 0xff);
            i = i + 1;
        }
    }

    final void checkWritable() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
    }

    /**
     * Compares this buffer with another for equality of their <em>remaining</em> bytes — what is
     * left to read, not the capacities and not the positions.
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is a {@code ByteBuffer} with the same remaining bytes
     */
    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof ByteBuffer) {
            ByteBuffer that = (ByteBuffer) other;
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
     * Returns a hash code over the remaining bytes, consistent with {@link #equals}.
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
     * Compares the remaining bytes of the two buffers lexicographically.
     *
     * @param that the buffer to compare with
     * @return a negative number, zero or a positive number as this buffer is less than, equal to
     *         or greater than {@code that}
     */
    public int compareTo(ByteBuffer that) {
        int n = remaining();
        if (that.remaining() < n) {
            n = that.remaining();
        }
        int result = remaining() - that.remaining();
        int i = 0;
        boolean decided = false;
        while (i < n && !decided) {
            int a = get(position() + i);
            int b = that.get(that.position() + i);
            if (a != b) {
                result = a - b;
                decided = true;
            }
            i = i + 1;
        }
        return result;
    }

    /**
     * Returns a summary of this buffer's indices — not its contents.
     *
     * @return a string of the form {@code ByteBuffer[pos=.. lim=.. cap=..]}
     */
    public String toString() {
        return "ByteBuffer[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]";
    }
}

/**
 * The heap implementation of {@link ByteBuffer}. Top-level and package-private rather than nested
 * — the project's idiom for a helper type — and skipped by the API-shape gate, which has no JDK
 * counterpart to compare it against.
 */
class HeapByteBuffer extends ByteBuffer {

    HeapByteBuffer(byte[] hb, int offset, int capacity) {
        super(hb, offset, capacity);
    }

    public ByteBuffer slice() {
        HeapByteBuffer b = new HeapByteBuffer(hb, offset + position(), remaining());
        b.readOnly = readOnly;
        b.bigEndian = bigEndian;
        return b;
    }

    public ByteBuffer slice(int index, int length) {
        HeapByteBuffer b = new HeapByteBuffer(hb, offset + index, length);
        b.readOnly = readOnly;
        b.bigEndian = bigEndian;
        return b;
    }

    public ByteBuffer duplicate() {
        HeapByteBuffer b = new HeapByteBuffer(hb, offset, capacity());
        b.position(position());
        b.limit(limit());
        b.readOnly = readOnly;
        b.bigEndian = bigEndian;
        return b;
    }

    public ByteBuffer asReadOnlyBuffer() {
        HeapByteBuffer b = (HeapByteBuffer) duplicate();
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

    public byte get() {
        return hb[offset + nextGetIndex()];
    }

    public byte get(int index) {
        return hb[offset + checkIndex(index)];
    }

    public ByteBuffer put(byte b) {
        checkWritable();
        hb[offset + nextPutIndex()] = b;
        return this;
    }

    public ByteBuffer put(int index, byte b) {
        checkWritable();
        hb[offset + checkIndex(index)] = b;
        return this;
    }
}
