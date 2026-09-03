package java.nio;

/**
 * A buffer of bytes, and the only one whose contents can be reinterpreted at other widths.
 *
 * <p>That asymmetry is the design of the whole package: bytes are what actually travel, and every
 * other buffer type is a <em>view</em> over some bytes — which is why the multi-byte accessors,
 * {@link ByteOrder} and the six {@code asXBuffer} factories live here and nowhere else.
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
 * <p><strong>What this class declares versus what it implements.</strong> Almost every accessor
 * below is {@code abstract}, exactly as in the JDK, and the implementation lives in the
 * package-private {@code HeapByteBuffer}. That is not decoration: the abstract class is the API,
 * and leaving the accessors abstract is what would let a direct buffer, a mapped buffer or a
 * read-only buffer be a different class without touching this one.
 *
 * <p><strong>Omitted from this implementation</strong> (the API-shape gate requires a subset, not
 * equality): everything reached through {@code Unsafe}, {@code MemorySegment} or a scoped memory
 * session — {@code heapSegment(...)} and the {@code MemorySegment} constructors —
 * because there is no off-heap memory here at all. {@link #alignmentOffset(int, int)} is
 * implemented but answers for a base address of zero, because a heap array has no address a
 * library can see; see its own note.
 */
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {

    final byte[] hb;
    final int offset;
    boolean isReadOnly;
    boolean bigEndian;

    /** Whether {@link #bigEndian} happens to agree with the machine — the JDK's fast-path flag. */
    boolean nativeByteOrder;

    ByteBuffer(byte[] hb, int offset, int capacity) {
        super(capacity);
        this.hb = hb;
        this.offset = offset;
        // The JDK's default is BIG_ENDIAN regardless of the machine — a deliberate choice, since a
        // buffer usually parses a FORMAT, and formats are mostly big-endian on the wire.
        this.bigEndian = true;
        // Both defaults are fixed and disagree: this class starts BIG_ENDIAN, and
        // ByteOrder.nativeOrder() is LITTLE_ENDIAN here. Written out rather than computed,
        // because computing it means a `getstatic` of ByteOrder.BIG_ENDIAN in the constructor of
        // every buffer ever created — a cost with nothing behind it, since nothing in this
        // implementation ever reads the flag. It is declared only because the JDK declares it,
        // where it selects an Unsafe fast path we do not have. Only order(ByteOrder), which
        // cannot avoid naming the constants, recomputes it.
        this.nativeByteOrder = false;
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
            throw Buffer.createCapacityException(capacity);
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
     * Writes a byte at the current position and advances it.
     *
     * @param b the byte to write
     * @return this buffer
     * @throws BufferOverflowException if the position is at the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer put(byte b);

    /**
     * Reads the byte at the given index without moving the position.
     *
     * @param index the index to read
     * @return the byte at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the limit
     */
    public abstract byte get(int index);

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
     * @throws IndexOutOfBoundsException if the {@code dst} range is out of bounds
     */
    public ByteBuffer get(byte[] dst, int off, int length) {
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
     * @throws BufferUnderflowException if fewer than {@code dst.length} bytes remain
     */
    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Copies bytes starting at an absolute index into part of an array, without moving the
     * position.
     *
     * @param index the first byte of this buffer to read
     * @param dst the array to copy into
     * @param off the first index of {@code dst} to write
     * @param length the number of bytes to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its container
     */
    public ByteBuffer get(int index, byte[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        return getArray(index, dst, off, length);
    }

    /**
     * Fills an array from this buffer starting at an absolute index, without moving the position.
     *
     * @param index the first byte of this buffer to read
     * @param dst the array to fill
     * @return this buffer
     * @throws IndexOutOfBoundsException if the range falls outside this buffer
     */
    public ByteBuffer get(int index, byte[] dst) {
        return get(index, dst, 0, dst.length);
    }

    /** The unchecked copy behind the absolute bulk {@code get}. */
    private ByteBuffer getArray(int index, byte[] dst, int off, int length) {
        int i = 0;
        while (i < length) {
            dst[off + i] = get(index + i);
            i = i + 1;
        }
        return this;
    }

    /**
     * Drains another buffer into this one: every byte remaining in {@code src} is written here,
     * and <em>both</em> positions advance. That both move is the point — this is the hand-off
     * between a buffer that was just filled and one about to be drained.
     *
     * @param src the buffer to copy from
     * @return this buffer
     * @throws BufferOverflowException if {@code src} has more remaining than this buffer
     * @throws IllegalArgumentException if {@code src} is this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(ByteBuffer src) {
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
        putBuffer(to, src, from, n);
        return this;
    }

    /**
     * Copies a range of another buffer into this one at an absolute index. Neither position
     * moves; both ranges are absolute.
     *
     * @param index the first byte of this buffer to write
     * @param src the buffer to copy from
     * @param off the first byte of {@code src} to read
     * @param length the number of bytes to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(int index, ByteBuffer src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        src.checkIndex(off, length);
        putBuffer(index, src, off, length);
        return this;
    }

    /** The unchecked copy behind {@link #put(int, ByteBuffer, int, int)}. */
    void putBuffer(int index, ByteBuffer src, int off, int length) {
        int i = 0;
        while (i < length) {
            put(index + i, src.get(off + i));
            i = i + 1;
        }
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
     * @throws BufferOverflowException if fewer than {@code src.length} bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * Copies part of an array into this buffer at an absolute index, without moving the position.
     *
     * @param index the first byte of this buffer to write
     * @param src the array to copy from
     * @param off the first index of {@code src} to read
     * @param length the number of bytes to copy
     * @return this buffer
     * @throws IndexOutOfBoundsException if either range falls outside its container
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(int index, byte[] src, int off, int length) {
        checkWritable();
        checkIndex(index, length);
        Buffer.checkBounds(off, length, src.length);
        return putArray(index, src, off, length);
    }

    /**
     * Copies a whole array into this buffer at an absolute index, without moving the position.
     *
     * @param index the first byte of this buffer to write
     * @param src the array to copy from
     * @return this buffer
     * @throws IndexOutOfBoundsException if the range falls outside this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public ByteBuffer put(int index, byte[] src) {
        return put(index, src, 0, src.length);
    }

    /** The unchecked copy behind the absolute bulk {@code put}. */
    ByteBuffer putArray(int index, byte[] src, int off, int length) {
        int i = 0;
        while (i < length) {
            put(index + i, src[off + i]);
            i = i + 1;
        }
        return this;
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
    public abstract ByteBuffer compact();

    /**
     * Tells whether this buffer's memory lives outside the Java heap.
     *
     * @return always {@code false} here — see {@link #allocateDirect(int)}
     */
    public abstract boolean isDirect();

    /**
     * Tells whether this buffer exposes an accessible backing array.
     *
     * @return {@code true} unless this buffer is read-only
     */
    public final boolean hasArray() {
        return hb != null && !isReadOnly;
    }

    /**
     * Returns the backing array.
     *
     * @return the array this buffer reads and writes
     * @throws ReadOnlyBufferException if this buffer is read-only
     * @throws UnsupportedOperationException if this buffer has no backing array
     */
    public final byte[] array() {
        if (hb == null) {
            throw new UnsupportedOperationException();
        }
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return hb;
    }

    /**
     * Returns the index in the backing array of this buffer's first byte.
     *
     * @return the offset of byte zero within the array
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public final int arrayOffset() {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return offset;
    }

    /**
     * Sets the position.
     *
     * <p>These seven overrides exist only to return the precise type, so a chain of buffer calls
     * does not decay to {@code Buffer}. The JDK writes them as {@code super.position(...)}; ours
     * call the package-private {@code setPosition} of {@link Buffer}, because our javac does not
     * emit {@code invokespecial} for {@code super.method()} yet.
     *
     * @param newPosition the new position
     * @return this buffer
     */
    public ByteBuffer position(int newPosition) {
        setPosition(newPosition);
        return this;
    }

    /**
     * Sets the limit.
     *
     * @param newLimit the new limit
     * @return this buffer
     */
    public ByteBuffer limit(int newLimit) {
        setLimit(newLimit);
        return this;
    }

    /**
     * Remembers the current position.
     *
     * @return this buffer
     */
    public ByteBuffer mark() {
        setMark();
        return this;
    }

    /**
     * Moves the position back to the mark.
     *
     * @return this buffer
     */
    public ByteBuffer reset() {
        resetToMark();
        return this;
    }

    /**
     * Resets the indices for writing from scratch.
     *
     * @return this buffer
     */
    public ByteBuffer clear() {
        clearIndices();
        return this;
    }

    /**
     * Turns a written buffer into a readable one.
     *
     * @return this buffer
     */
    public ByteBuffer flip() {
        flipIndices();
        return this;
    }

    /**
     * Rewinds to position zero, keeping the limit.
     *
     * @return this buffer
     */
    public ByteBuffer rewind() {
        rewindIndices();
        return this;
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
     * once and stop thinking about it. Note that a view already handed out by one of the
     * {@code asXBuffer} methods keeps the order it was created with; this does not reach it.
     *
     * @param bo the order to use
     * @return this buffer
     */
    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = bo == ByteOrder.BIG_ENDIAN;
        nativeByteOrder = bigEndian == (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
        return this;
    }

    /**
     * Returns how far {@code index} is past the previous {@code unitSize}-aligned address.
     *
     * <p><strong>Answers for a base address of zero.</strong> In the JDK this is
     * {@code (address + index) % unitSize}, where {@code address} is where the memory actually
     * begins; for a heap buffer that is inside the array object and only {@code Unsafe} can see
     * it. KajiLibrary has no {@code Unsafe}, so the only defensible base is zero, and the answer
     * is {@code index % unitSize}. That makes the method self-consistent — {@link #alignedSlice}
     * built on it does return a slice whose element indices are aligned — but it does <em>not</em>
     * say anything about the machine addresses the bytes really live at, which is what the method
     * is for on a direct buffer. It is here so that code written against the API compiles and
     * behaves sensibly, not because the alignment is real.
     *
     * @param index a byte index into this buffer
     * @param unitSize the alignment to measure against, a power of two
     * @return the number of bytes from the previous aligned index, in {@code [0, unitSize)}
     * @throws IllegalArgumentException if {@code index} is negative or {@code unitSize} is not a
     *         power of two
     * @throws UnsupportedOperationException if {@code unitSize > 8} and this buffer is not direct
     */
    public final int alignmentOffset(int index, int unitSize) {
        if (index < 0) {
            throw new IllegalArgumentException("Index less than zero: " + index);
        }
        if (unitSize < 1 || (unitSize & (unitSize - 1)) != 0) {
            throw new IllegalArgumentException("Unit size not a power of two: " + unitSize);
        }
        if (unitSize > 8 && !isDirect()) {
            throw new UnsupportedOperationException(
                    "Unit size unsupported for non-direct buffers: " + unitSize);
        }
        return index % unitSize;
    }

    /**
     * Creates a slice of this buffer whose first byte is aligned to {@code unitSize} and whose
     * length is a whole number of units.
     *
     * <p>The point on a direct buffer is that a {@code getLong} inside the slice never straddles
     * a cache line or a page; here it is the index arithmetic without the hardware meaning, for
     * the reason given on {@link #alignmentOffset(int, int)}.
     *
     * @param unitSize the alignment, a power of two
     * @return the aligned slice, possibly empty
     * @throws IllegalArgumentException if {@code unitSize} is not a power of two
     * @throws UnsupportedOperationException if {@code unitSize > 8} and this buffer is not direct
     */
    public final ByteBuffer alignedSlice(int unitSize) {
        int pos = position();
        int lim = limit();
        int posMod = alignmentOffset(pos, unitSize);
        int limMod = alignmentOffset(lim, unitSize);
        int alignedPos = pos;
        if (posMod > 0) {
            alignedPos = pos + (unitSize - posMod);
        }
        int alignedLim = lim - limMod;
        if (alignedPos > lim || alignedLim < pos) {
            alignedPos = pos;
            alignedLim = pos;
        }
        return slice(alignedPos, alignedLim - alignedPos);
    }

    /**
     * Reads two bytes as a char at the current position, advancing it.
     *
     * @return the char read
     * @throws BufferUnderflowException if fewer than two bytes remain
     */
    public abstract char getChar();

    /**
     * Writes a char as two bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than two bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putChar(char value);

    /**
     * Reads two bytes as a char at the given index.
     *
     * @param index the index of the first byte
     * @return the char read
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     */
    public abstract char getChar(int index);

    /**
     * Writes a char as two bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putChar(int index, char value);

    /**
     * Creates a char view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 2} chars sharing these bytes, with this buffer's
     *         current order frozen in
     */
    public abstract CharBuffer asCharBuffer();

    /**
     * Reads two bytes as a short at the current position, advancing it.
     *
     * @return the short read
     * @throws BufferUnderflowException if fewer than two bytes remain
     */
    public abstract short getShort();

    /**
     * Writes a short as two bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than two bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putShort(short value);

    /**
     * Reads two bytes as a short at the given index.
     *
     * @param index the index of the first byte
     * @return the short read
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     */
    public abstract short getShort(int index);

    /**
     * Writes a short as two bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the two bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putShort(int index, short value);

    /**
     * Creates a short view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 2} shorts sharing these bytes
     */
    public abstract ShortBuffer asShortBuffer();

    /**
     * Reads four bytes as an int at the current position, advancing it.
     *
     * @return the int read
     * @throws BufferUnderflowException if fewer than four bytes remain
     */
    public abstract int getInt();

    /**
     * Writes an int as four bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than four bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putInt(int value);

    /**
     * Reads four bytes as an int at the given index.
     *
     * @param index the index of the first byte
     * @return the int read
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     */
    public abstract int getInt(int index);

    /**
     * Writes an int as four bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putInt(int index, int value);

    /**
     * Creates an int view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 4} ints sharing these bytes
     */
    public abstract IntBuffer asIntBuffer();

    /**
     * Reads eight bytes as a long at the current position, advancing it.
     *
     * @return the long read
     * @throws BufferUnderflowException if fewer than eight bytes remain
     */
    public abstract long getLong();

    /**
     * Writes a long as eight bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than eight bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putLong(long value);

    /**
     * Reads eight bytes as a long at the given index.
     *
     * @param index the index of the first byte
     * @return the long read
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     */
    public abstract long getLong(int index);

    /**
     * Writes a long as eight bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putLong(int index, long value);

    /**
     * Creates a long view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 8} longs sharing these bytes
     */
    public abstract LongBuffer asLongBuffer();

    /**
     * Reads four bytes as a float at the current position, advancing it.
     *
     * <p>The IEEE-754 conversion is done in pure Java by {@code ByteCodec}, because the VM binds
     * no implementation to {@code Double.doubleToLongBits} and {@code Float} has no bit accessors
     * at all. It is bit-exact; see that class for why it is slower than it should be.
     *
     * @return the float read
     * @throws BufferUnderflowException if fewer than four bytes remain
     */
    public abstract float getFloat();

    /**
     * Writes a float as four bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than four bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putFloat(float value);

    /**
     * Reads four bytes as a float at the given index.
     *
     * @param index the index of the first byte
     * @return the float read
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     */
    public abstract float getFloat(int index);

    /**
     * Writes a float as four bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the four bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putFloat(int index, float value);

    /**
     * Creates a float view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 4} floats sharing these bytes
     */
    public abstract FloatBuffer asFloatBuffer();

    /**
     * Reads eight bytes as a double at the current position, advancing it.
     *
     * @return the double read
     * @throws BufferUnderflowException if fewer than eight bytes remain
     */
    public abstract double getDouble();

    /**
     * Writes a double as eight bytes at the current position, advancing it.
     *
     * @param value the value to write
     * @return this buffer
     * @throws BufferOverflowException if fewer than eight bytes of room remain
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putDouble(double value);

    /**
     * Reads eight bytes as a double at the given index.
     *
     * @param index the index of the first byte
     * @return the double read
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     */
    public abstract double getDouble(int index);

    /**
     * Writes a double as eight bytes at the given index.
     *
     * @param index the index of the first byte
     * @param value the value to write
     * @return this buffer
     * @throws IndexOutOfBoundsException if the eight bytes are not within the limit
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public abstract ByteBuffer putDouble(int index, double value);

    /**
     * Creates a double view of this buffer's remaining bytes.
     *
     * @return a buffer of {@code remaining() / 8} doubles sharing these bytes
     */
    public abstract DoubleBuffer asDoubleBuffer();

    /**
     * Returns the array this buffer's bytes live in, or {@code null} for a buffer that owns none.
     *
     * <p>Not the same question as {@link #array()}: that one is the caller-facing accessor and
     * refuses to answer for a read-only buffer, while this is the implementation's own handle on
     * the storage and always tells the truth.
     *
     * @return the backing array, or {@code null}
     */
    Object base() {
        return (Object) hb;
    }

    final void checkWritable() {
        if (isReadOnly) {
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

    /** Signed comparison of two bytes, the ordering {@link #compareTo} is defined by. */
    private static int compare(byte x, byte y) {
        return x - y;
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
            int c = compare(get(position() + i), that.get(that.position() + i));
            if (c != 0) {
                result = c;
                decided = true;
            }
            i = i + 1;
        }
        return result;
    }

    /**
     * Returns the index of the first byte where this buffer and {@code that} disagree, relative
     * to their positions, or {@code -1} if the shared prefix runs to the end of both.
     *
     * <p>When one is a strict prefix of the other the answer is the shorter length: the buffers
     * do differ, and the first place they do is where the shorter one ended.
     *
     * @param that the buffer to compare with
     * @return the relative index of the first mismatch, or {@code -1}
     */
    public int mismatch(ByteBuffer that) {
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
     * @return a string of the form {@code ByteBuffer[pos=.. lim=.. cap=..]}
     */
    public String toString() {
        return "ByteBuffer[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]";
    }
}

/**
 * The heap implementation of {@link ByteBuffer}. Top-level and package-private rather than nested
 * — the project's idiom for a helper type.
 *
 * <p>This is where every accessor the abstract class leaves open actually touches the array. The
 * multi-byte ones all funnel through {@code ByteCodec}, so the byte order lives in exactly one
 * place; the {@code asXBuffer} factories hand out a {@code ByteViewXBuffer} over the same array.
 */
class HeapByteBuffer extends ByteBuffer {

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
    protected HeapByteBuffer(byte[] buf, int mark, int pos, int lim, int cap, int off,
            java.lang.foreign.MemorySegment segment) {
        super(buf, off, cap);
        this.limit(lim);
        if (mark >= 0) {
            this.position(mark);
            this.mark();
        }
        this.position(pos);
    }

    HeapByteBuffer(byte[] hb, int offset, int capacity) {
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

    /**
     * Returns the shift that turns an element index into a byte offset.
     *
     * @return zero — for a byte buffer the two are the same number
     */
    int scaleShifts() {
        return 0;
    }

    /**
     * Reads one byte by buffer index, with no bounds check and no read-only check.
     *
     * <p>The checked accessors validate first and then land here; the view buffers handed out by
     * {@code asCharBuffer} and friends reach for it directly, having already validated in their
     * own element units.
     *
     * @param i the buffer index
     * @return the byte at {@code i}
     */
    byte _get(int i) {
        return hb[ix(i)];
    }

    /**
     * Writes one byte by buffer index, with no bounds check and no read-only check.
     *
     * @param i the buffer index
     * @param b the byte to write
     */
    void _put(int i, byte b) {
        hb[ix(i)] = b;
    }

    /** Reads {@code count} bytes at buffer index {@code at}, in this buffer's order. */
    private long readBits(int at, int count) {
        return ByteCodec.read(hb, ix(at), count, bigEndian);
    }

    /** Writes {@code count} bytes at buffer index {@code at}, in this buffer's order. */
    private void writeBits(int at, int count, long value) {
        checkWritable();
        ByteCodec.write(hb, ix(at), count, bigEndian, value);
    }

    public ByteBuffer slice() {
        HeapByteBuffer b = new HeapByteBuffer(hb, offset + position(), remaining());
        b.isReadOnly = isReadOnly;
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    public ByteBuffer slice(int index, int length) {
        checkIndex(index, length);
        HeapByteBuffer b = new HeapByteBuffer(hb, offset + index, length);
        b.isReadOnly = isReadOnly;
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    public ByteBuffer duplicate() {
        HeapByteBuffer b = new HeapByteBuffer(hb, offset, capacity());
        b.position(position());
        b.limit(limit());
        b.isReadOnly = isReadOnly;
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    public ByteBuffer asReadOnlyBuffer() {
        HeapByteBuffer b = (HeapByteBuffer) duplicate();
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

    public byte get() {
        return _get(nextGetIndex());
    }

    public byte get(int index) {
        return _get(checkIndex(index));
    }

    public ByteBuffer put(byte b) {
        checkWritable();
        _put(nextPutIndex(), b);
        return this;
    }

    public ByteBuffer put(int index, byte b) {
        checkWritable();
        _put(checkIndex(index), b);
        return this;
    }

    public ByteBuffer get(byte[] dst, int off, int length) {
        Buffer.checkBounds(off, length, dst.length);
        int from = nextGetIndex(length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + from + i];
            i = i + 1;
        }
        return this;
    }

    public ByteBuffer get(int index, byte[] dst, int off, int length) {
        checkIndex(index, length);
        Buffer.checkBounds(off, length, dst.length);
        int i = 0;
        while (i < length) {
            dst[off + i] = hb[offset + index + i];
            i = i + 1;
        }
        return this;
    }

    public ByteBuffer put(byte[] src, int off, int length) {
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

    public ByteBuffer put(int index, byte[] src, int off, int length) {
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
    public ByteBuffer put(ByteBuffer src) {
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
    public ByteBuffer put(int index, ByteBuffer src, int off, int length) {
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

    public char getChar() {
        return (char) readBits(nextGetIndex(2), 2);
    }

    public ByteBuffer putChar(char value) {
        writeBits(nextPutIndex(2), 2, (long) value);
        return this;
    }

    public char getChar(int index) {
        return (char) readBits(checkIndex(index, 2), 2);
    }

    public ByteBuffer putChar(int index, char value) {
        writeBits(checkIndex(index, 2), 2, (long) value);
        return this;
    }

    public CharBuffer asCharBuffer() {
        return new ByteViewCharBuffer(hb, offset + position(), remaining() / 2, bigEndian,
                isReadOnly);
    }

    public short getShort() {
        return (short) readBits(nextGetIndex(2), 2);
    }

    public ByteBuffer putShort(short value) {
        writeBits(nextPutIndex(2), 2, (long) value);
        return this;
    }

    public short getShort(int index) {
        return (short) readBits(checkIndex(index, 2), 2);
    }

    public ByteBuffer putShort(int index, short value) {
        writeBits(checkIndex(index, 2), 2, (long) value);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        return new ByteViewShortBuffer(hb, offset + position(), remaining() / 2, bigEndian,
                isReadOnly);
    }

    public int getInt() {
        return (int) readBits(nextGetIndex(4), 4);
    }

    public ByteBuffer putInt(int value) {
        writeBits(nextPutIndex(4), 4, (long) value);
        return this;
    }

    public int getInt(int index) {
        return (int) readBits(checkIndex(index, 4), 4);
    }

    public ByteBuffer putInt(int index, int value) {
        writeBits(checkIndex(index, 4), 4, (long) value);
        return this;
    }

    public IntBuffer asIntBuffer() {
        return new ByteViewIntBuffer(hb, offset + position(), remaining() / 4, bigEndian,
                isReadOnly);
    }

    public long getLong() {
        return readBits(nextGetIndex(8), 8);
    }

    public ByteBuffer putLong(long value) {
        writeBits(nextPutIndex(8), 8, value);
        return this;
    }

    public long getLong(int index) {
        return readBits(checkIndex(index, 8), 8);
    }

    public ByteBuffer putLong(int index, long value) {
        writeBits(checkIndex(index, 8), 8, value);
        return this;
    }

    public LongBuffer asLongBuffer() {
        return new ByteViewLongBuffer(hb, offset + position(), remaining() / 8, bigEndian,
                isReadOnly);
    }

    public float getFloat() {
        return ByteCodec.bitsToFloat((int) readBits(nextGetIndex(4), 4));
    }

    public ByteBuffer putFloat(float value) {
        writeBits(nextPutIndex(4), 4, (long) ByteCodec.floatToBits(value));
        return this;
    }

    public float getFloat(int index) {
        return ByteCodec.bitsToFloat((int) readBits(checkIndex(index, 4), 4));
    }

    public ByteBuffer putFloat(int index, float value) {
        writeBits(checkIndex(index, 4), 4, (long) ByteCodec.floatToBits(value));
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        return new ByteViewFloatBuffer(hb, offset + position(), remaining() / 4, bigEndian,
                isReadOnly);
    }

    public double getDouble() {
        return ByteCodec.bitsToDouble(readBits(nextGetIndex(8), 8));
    }

    public ByteBuffer putDouble(double value) {
        writeBits(nextPutIndex(8), 8, ByteCodec.doubleToBits(value));
        return this;
    }

    public double getDouble(int index) {
        return ByteCodec.bitsToDouble(readBits(checkIndex(index, 8), 8));
    }

    public ByteBuffer putDouble(int index, double value) {
        writeBits(checkIndex(index, 8), 8, ByteCodec.doubleToBits(value));
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        return new ByteViewDoubleBuffer(hb, offset + position(), remaining() / 8, bigEndian,
                isReadOnly);
    }
}
