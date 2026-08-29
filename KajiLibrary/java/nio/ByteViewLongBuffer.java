package java.nio;

/**
 * A {@link LongBuffer} that owns no array: every element is decoded from, and encoded into, the
 * bytes of a {@link ByteBuffer}. This is what {@link ByteBuffer#asLongBuffer()} returns.
 *
 * <p>A view is the other half of the package's central idea. A {@code ByteBuffer} is the only
 * buffer whose contents can be reinterpreted at another width, and a view is that
 * reinterpretation made into an object: {@code capacity} counts longs rather than bytes, the
 * indices move by elements, and the byte order is <em>frozen at creation</em> — changing the
 * order of the byte buffer afterwards does not reach a view already handed out, which is the
 * JDK's behaviour too and the reason it has a separate class per direction.
 *
 * <p>The bytes are shared, not copied: writing through the view is visible through the byte
 * buffer and the other way round.
 *
 * <p>Deliberately <strong>not</strong> a clone of the JDK's internal
 * {@code ByteBufferAsLongBufferB} / {@code ...L} / {@code ...RB} / {@code ...RL} quartet. Those
 * four classes exist to specialise the byte order and the read-only-ness into the type, which is
 * a performance decision inside HotSpot, not part of the API; one class with two flags says the
 * same thing. The name differs on purpose so that nothing here can be mistaken for a copy of
 * {@code java.base}'s internals.
 */
class ByteViewLongBuffer extends LongBuffer {

    /** The byte buffer's array — shared, never copied. */
    private final byte[] bytes;

    /** The index in {@link #bytes} of this view's element zero. */
    private final int byteOffset;

    /** The order this view was created with; it does not follow the byte buffer afterwards. */
    private final boolean bigEndian;

    ByteViewLongBuffer(byte[] bytes, int byteOffset, int capacity, boolean bigEndian,
                      boolean isReadOnly) {
        super(null, 0, capacity);
        this.bytes = bytes;
        this.byteOffset = byteOffset;
        this.bigEndian = bigEndian;
        this.isReadOnly = isReadOnly;
    }

    /** Reads one element from the byte at index {@code at}. */
    private long decode(int at) {
        return ByteCodec.read(bytes, at, 8, bigEndian);
    }

    /** Writes one element into the bytes starting at index {@code at}. */
    private void encode(int at, long value) {
        ByteCodec.write(bytes, at, 8, bigEndian, value);
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isDirect() {
        return false;
    }

    /**
     * Returns the order this view decodes with.
     *
     * <p>Unlike a standalone buffer, whose order is the platform's and carries no information, a
     * view's order is the one thing about it that had to be decided.
     *
     * @return the order frozen into this view when it was created
     */
    public ByteOrder order() {
        if (bigEndian) {
            return ByteOrder.BIG_ENDIAN;
        }
        return ByteOrder.LITTLE_ENDIAN;
    }

    public long get() {
        return decode(byteOffset + 8 * nextGetIndex());
    }

    public long get(int index) {
        return decode(byteOffset + 8 * checkIndex(index));
    }

    public LongBuffer put(long value) {
        checkWritable();
        encode(byteOffset + 8 * nextPutIndex(), value);
        return this;
    }

    public LongBuffer put(int index, long value) {
        checkWritable();
        encode(byteOffset + 8 * checkIndex(index), value);
        return this;
    }

    public LongBuffer slice() {
        return new ByteViewLongBuffer(bytes, byteOffset + 8 * position(), remaining(), bigEndian,
                isReadOnly);
    }

    public LongBuffer slice(int index, int length) {
        checkIndex(index, length);
        return new ByteViewLongBuffer(bytes, byteOffset + 8 * index, length, bigEndian,
                isReadOnly);
    }

    public LongBuffer duplicate() {
        ByteViewLongBuffer b =
                new ByteViewLongBuffer(bytes, byteOffset, capacity(), bigEndian, isReadOnly);
        b.position(position());
        b.limit(limit());
        return b;
    }

    public LongBuffer asReadOnlyBuffer() {
        ByteViewLongBuffer b =
                new ByteViewLongBuffer(bytes, byteOffset, capacity(), bigEndian, true);
        b.position(position());
        b.limit(limit());
        return b;
    }

    /**
     * Slides the unread elements to the front, element by element through the bytes.
     *
     * <p>Written with the codec inline rather than through {@code get}/{@code put} so that a
     * compaction is one pass over the bytes with no virtual calls per element.
     *
     * @return this buffer
     * @throws ReadOnlyBufferException if this buffer is read-only
     */
    public LongBuffer compact() {
        checkWritable();
        int n = remaining();
        int from = position();
        int i = 0;
        while (i < n) {
            encode(byteOffset + 8 * i, decode(byteOffset + 8 * (from + i)));
            i = i + 1;
        }
        position(n);
        limit(capacity());
        return this;
    }
}
