package java.nio;

/**
 * A {@link CharBuffer} that owns no array: every element is decoded from, and encoded into, the
 * bytes of a {@link ByteBuffer}. This is what {@link ByteBuffer#asCharBuffer()} returns.
 *
 * <p>A view is the other half of the package's central idea. A {@code ByteBuffer} is the only
 * buffer whose contents can be reinterpreted at another width, and a view is that
 * reinterpretation made into an object: {@code capacity} counts chars rather than bytes, the
 * indices move by elements, and the byte order is <em>frozen at creation</em> — changing the
 * order of the byte buffer afterwards does not reach a view already handed out, which is the
 * JDK's behaviour too and the reason it has a separate class per direction.
 *
 * <p>The bytes are shared, not copied: writing through the view is visible through the byte
 * buffer and the other way round.
 *
 * <p>Deliberately <strong>not</strong> a clone of the JDK's internal
 * {@code ByteBufferAsCharBufferB} / {@code ...L} / {@code ...RB} / {@code ...RL} quartet. Those
 * four classes exist to specialise the byte order and the read-only-ness into the type, which is
 * a performance decision inside HotSpot, not part of the API; one class with two flags says the
 * same thing. The name differs on purpose so that nothing here can be mistaken for a copy of
 * {@code java.base}'s internals.
 */
class ByteViewCharBuffer extends CharBuffer {

    /** The byte buffer's array — shared, never copied. */
    private final byte[] bytes;

    /** The index in {@link #bytes} of this view's element zero. */
    private final int byteOffset;

    /** The order this view was created with; it does not follow the byte buffer afterwards. */
    private final boolean bigEndian;

    ByteViewCharBuffer(byte[] bytes, int byteOffset, int capacity, boolean bigEndian,
                      boolean isReadOnly) {
        super(null, 0, capacity);
        this.bytes = bytes;
        this.byteOffset = byteOffset;
        this.bigEndian = bigEndian;
        this.isReadOnly = isReadOnly;
    }

    /** Reads one element from the byte at index {@code at}. */
    private char decode(int at) {
        return (char) ByteCodec.read(bytes, at, 2, bigEndian);
    }

    /** Writes one element into the bytes starting at index {@code at}. */
    private void encode(int at, char value) {
        ByteCodec.write(bytes, at, 2, bigEndian, (long) value);
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

    public char get() {
        return decode(byteOffset + 2 * nextGetIndex());
    }

    public char get(int index) {
        return decode(byteOffset + 2 * checkIndex(index));
    }

    char getUnchecked(int index) {
        return decode(byteOffset + 2 * index);
    }

    /**
     * @return the order frozen into this view — unlike a buffer over a {@link CharSequence},
     *         this one's characters really are a region of bytes
     */
    ByteOrder charRegionOrder() {
        return order();
    }

    public CharBuffer put(char value) {
        checkWritable();
        encode(byteOffset + 2 * nextPutIndex(), value);
        return this;
    }

    public CharBuffer put(int index, char value) {
        checkWritable();
        encode(byteOffset + 2 * checkIndex(index), value);
        return this;
    }

    public CharBuffer slice() {
        return new ByteViewCharBuffer(bytes, byteOffset + 2 * position(), remaining(), bigEndian,
                isReadOnly);
    }

    public CharBuffer slice(int index, int length) {
        checkIndex(index, length);
        return new ByteViewCharBuffer(bytes, byteOffset + 2 * index, length, bigEndian,
                isReadOnly);
    }

    public CharBuffer duplicate() {
        ByteViewCharBuffer b =
                new ByteViewCharBuffer(bytes, byteOffset, capacity(), bigEndian, isReadOnly);
        b.position(position());
        b.limit(limit());
        return b;
    }

    public CharBuffer asReadOnlyBuffer() {
        ByteViewCharBuffer b =
                new ByteViewCharBuffer(bytes, byteOffset, capacity(), bigEndian, true);
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
    public CharBuffer subSequence(int start, int end) {
        return subSequenceImpl(start, end);
    }

    String toString(int start, int end) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < end) {
            sb.append(decode(byteOffset + 2 * i));
            i = i + 1;
        }
        return sb.toString();
    }

    public CharBuffer compact() {
        checkWritable();
        int n = remaining();
        int from = position();
        int i = 0;
        while (i < n) {
            encode(byteOffset + 2 * i, decode(byteOffset + 2 * (from + i)));
            i = i + 1;
        }
        position(n);
        limit(capacity());
        return this;
    }
}
