package javax.imageio.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.ImageInputStreamImpl -- all of {@link ImageInputStream} except
 * access to the data.
 *
 * <p>A subclass only has to supply {@link #read()} and {@link #read(byte[], int, int)}; the rest
 * --the two dozen {@code readX}, the byte order, the bits, the marks-- comes from here. (An earlier
 * note said twenty.)
 *
 * <h2>The subclass has to maintain {@link #streamPos}</h2>
 *
 * <p>It is the contract that gets forgotten. The two abstract {@code read} have to <b>add</b> to
 * {@code streamPos} what they read: this class does not do it for them, because it does not know
 * how far they advanced.
 *
 * <p>And they have to call {@link #checkClosed} before touching anything.
 *
 * <h2>The bit offset</h2>
 *
 * <p>{@link #bitOffset} is what makes it possible to read fields that do not fall on byte
 * boundaries. This class applies the rule: every read of one byte or more sets it to zero, so
 * alternating between bits and bytes works without keeping count.
 *
 * <p>{@link #readBits} reads one bit per iteration. It is the simple and correct version; one that
 * gathered whole bytes would be faster and quite a bit easier to break at the edges.
 *
 * <h2>Marks and flushing</h2>
 *
 * <p>{@link #mark} pushes positions and {@link #reset} pops them; see {@link ImageInputStream}. And
 * {@link #flushedPos} is the barrier: nothing before it can be read again, and {@link #seek}
 * backwards past it throws {@link IndexOutOfBoundsException}.
 *
 * <p>{@link #close} does <b>not</b> close the underlying stream; see {@link
 * ImageInputStream#close}.
 */
public abstract class ImageInputStreamImpl implements ImageInputStream {

    /**
     * Eight bytes of scaffolding for the {@code readX}.
     *
     * <p>Package-private and shared between calls: it avoids allocating an array for each {@code
     * readInt}, and an image reader does millions. It is not thread-safe, and the JDK does not
     * promise that either.
     */
    byte[] byteBuf = new byte[8];

    /** Which order to read multi-byte values in. */
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    /** At which byte reading is. The subclass maintains it; see the class note. */
    protected long streamPos;

    /** At which bit within that byte. */
    protected int bitOffset;

    /** How far it was flushed. */
    protected long flushedPos;

    /** Whether it was already closed. */
    private boolean isClosed = false;

    /** The marks, stacked. */
    private final ArrayList<Long> markByteStack = new ArrayList<Long>();

    /** The bit offsets of each mark. */
    private final ArrayList<Integer> markBitStack = new ArrayList<Integer>();

    /** For the subclasses. */
    public ImageInputStreamImpl() {
    }

    /**
     * Fails if the stream is closed.
     *
     * <p>Every subclass has to call it at the start of its {@code read}.
     *
     * @throws IOException if it was already closed
     */
    protected final void checkClosed() throws IOException {
        if (this.isClosed) {
            throw new IOException("closed");
        }
    }

    /** Which order to read in. */
    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /** Which one is set. */
    public ByteOrder getByteOrder() {
        return this.byteOrder;
    }

    /** The subclass has to supply it. See the class note. */
    public abstract int read() throws IOException;

    /** Until the array is full. */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /** The subclass has to supply it. */
    public abstract int read(byte[] b, int off, int len) throws IOException;

    /**
     * Up to {@code len} bytes, without copying.
     *
     * <p>This implementation <b>does</b> copy: it allocates an array of the requested size and
     * hands it over. The gain of not copying depends on the subclass having a buffer of its own to
     * lend from, and this class has none. It is what the JDK does in this same class.
     *
     * @throws IndexOutOfBoundsException if the length is negative
     * @throws NullPointerException if the buffer is null
     */
    public void readBytes(IIOByteBuffer buf, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException("buf == null!");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len < 0!");
        }
        byte[] data = new byte[len];
        len = read(data, 0, len);
        buf.setData(data);
        buf.setOffset(0);
        buf.setLength(len);
    }

    /** One byte as a boolean. */
    public boolean readBoolean() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch != 0;
    }

    /** One signed byte. */
    public byte readByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    /** One unsigned byte. */
    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /** Two signed bytes, in the configured order. */
    public short readShort() throws IOException {
        readFullyInternal(2);
        int hi = this.byteBuf[0] & 0xFF;
        int lo = this.byteBuf[1] & 0xFF;
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short) ((hi << 8) | lo);
        }
        return (short) ((lo << 8) | hi);
    }

    /** Two unsigned bytes. */
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    /** Two bytes as a char. */
    public char readChar() throws IOException {
        return (char) readShort();
    }

    /** Four signed bytes. */
    public int readInt() throws IOException {
        readFullyInternal(4);
        int b0 = this.byteBuf[0] & 0xFF;
        int b1 = this.byteBuf[1] & 0xFF;
        int b2 = this.byteBuf[2] & 0xFF;
        int b3 = this.byteBuf[3] & 0xFF;
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        }
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    /** Four unsigned bytes. See {@link ImageInputStream#readUnsignedInt}. */
    public long readUnsignedInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    /** Eight bytes. */
    public long readLong() throws IOException {
        // Built from two four-byte ints and not byte by byte: that way the byte order is applied
        // once, in readInt, instead of repeating the logic.
        int i1 = readInt();
        int i2 = readInt();
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return ((long) i1 << 32) + (i2 & 0xFFFFFFFFL);
        }
        return ((long) i2 << 32) + (i1 & 0xFFFFFFFFL);
    }

    /** Four bytes as floating point. */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /** Eight bytes as floating point. */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /** A line, one byte per character. See {@link ImageInputStream#readLine}. */
    public String readLine() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            c = read();
            if (c == -1 || c == '\n') {
                eol = true;
            } else if (c == '\r') {
                eol = true;
                // A \r\n counts as a single line end, and the \n is not consumed if it does not
                // come.
                long cur = getStreamPosition();
                if (read() != '\n') {
                    seek(cur);
                }
            } else {
                input.append((char) c);
            }
        }
        if (c == -1 && input.length() == 0) {
            return null;
        }
        return input.toString();
    }

    /**
     * A string in modified UTF-8.
     *
     * <p>Always in network order: the order is switched, the string read, and the order restored --
     * even if the read fails. See {@link ImageInputStream#readUTF}.
     */
    public String readUTF() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        ByteOrder oldByteOrder = getByteOrder();
        setByteOrder(ByteOrder.BIG_ENDIAN);
        String ret;
        try {
            ret = DataInputStream.readUTF(this);
        } catch (IOException e) {
            setByteOrder(oldByteOrder);
            throw e;
        }
        setByteOrder(oldByteOrder);
        return ret;
    }

    /** Fills that part of the array. */
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        while (len > 0) {
            int nbytes = read(b, off, len);
            if (nbytes == -1) {
                throw new EOFException();
            }
            off = off + nbytes;
            len = len - nbytes;
        }
    }

    /** Fills the array. */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /** Fills that part, two bytes per element. */
    public void readFully(short[] s, int off, int len) throws IOException {
        checkBounds(off, len, s.length);
        int i = 0;
        while (i < len) {
            s[off + i] = readShort();
            i = i + 1;
        }
    }

    /** Same, with chars. */
    public void readFully(char[] c, int off, int len) throws IOException {
        checkBounds(off, len, c.length);
        int i = 0;
        while (i < len) {
            c[off + i] = readChar();
            i = i + 1;
        }
    }

    /** Same, four bytes per element. */
    public void readFully(int[] i, int off, int len) throws IOException {
        checkBounds(off, len, i.length);
        int k = 0;
        while (k < len) {
            i[off + k] = readInt();
            k = k + 1;
        }
    }

    /** Same, eight bytes. */
    public void readFully(long[] l, int off, int len) throws IOException {
        checkBounds(off, len, l.length);
        int i = 0;
        while (i < len) {
            l[off + i] = readLong();
            i = i + 1;
        }
    }

    /** Same, four-byte floating point. */
    public void readFully(float[] f, int off, int len) throws IOException {
        checkBounds(off, len, f.length);
        int i = 0;
        while (i < len) {
            f[off + i] = readFloat();
            i = i + 1;
        }
    }

    /** Same, eight-byte. */
    public void readFully(double[] d, int off, int len) throws IOException {
        checkBounds(off, len, d.length);
        int i = 0;
        while (i < len) {
            d[off + i] = readDouble();
            i = i + 1;
        }
    }

    /** At which byte it is. */
    public long getStreamPosition() throws IOException {
        checkClosed();
        return this.streamPos;
    }

    /** At which bit within that byte. */
    public int getBitOffset() throws IOException {
        checkClosed();
        return this.bitOffset;
    }

    /**
     * Sets it.
     *
     * @throws IllegalArgumentException if it is not between 0 and 7
     */
    public void setBitOffset(int bitOffset) throws IOException {
        checkClosed();
        if (bitOffset < 0 || bitOffset > 7) {
            throw new IllegalArgumentException("bitOffset must be betwwen 0 and 7!");
        }
        this.bitOffset = bitOffset;
    }

    /**
     * One bit.
     *
     * <p>It reads the byte, takes out the bit that is due, and if it was not the last one of the
     * byte it <b>goes back</b> so that the next read finds the same byte. It is what makes reading
     * eight bits in a row consume one byte and not eight.
     */
    public int readBit() throws IOException {
        checkClosed();
        int bo = this.bitOffset;
        int value = read();
        if (value == -1) {
            throw new EOFException();
        }
        value = (value >> (7 - bo)) & 0x1;
        bo = bo + 1;
        this.bitOffset = bo & 0x7;
        if (this.bitOffset != 0) {
            seek(getStreamPosition() - 1);
            this.bitOffset = bo & 0x7;
        }
        return value;
    }

    /**
     * Up to 64 bits.
     *
     * <p>One bit at a time: it is the simple version, and the one that does not get the edges
     * wrong.
     *
     * @throws IllegalArgumentException if more than 64 are asked for
     */
    public long readBits(int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException();
        }
        if (numBits == 0) {
            return 0L;
        }
        long accum = 0L;
        int i = 0;
        while (i < numBits) {
            accum = (accum << 1) | readBit();
            i = i + 1;
        }
        return accum;
    }

    /** Unknown; a subclass that can know redefines it. */
    public long length() {
        return -1L;
    }

    /** Skips bytes. */
    public int skipBytes(int n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return (int) (getStreamPosition() - pos);
    }

    /** Same, with a large skip. */
    public long skipBytes(long n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return getStreamPosition() - pos;
    }

    /**
     * Seeks to that byte.
     *
     * <p>It clears the bit offset, like every byte operation.
     *
     * @throws IndexOutOfBoundsException if it is before the flushed position
     */
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.streamPos = pos;
    }

    /** Pushes the position and the bit offset. */
    public void mark() {
        try {
            this.markByteStack.add(Long.valueOf(getStreamPosition()));
            this.markBitStack.add(Integer.valueOf(getBitOffset()));
        } catch (IOException e) {
            // The stream is closed. `mark` declares no IOException, so there is nowhere to report
            // it; the matching `reset` will fail, which is where the error can be reported.
        }
    }

    /**
     * Pops the last mark.
     *
     * <p>Without marks it does nothing: it is what the JDK does, and not throwing here allows a
     * defensive {@code reset}.
     */
    public void reset() throws IOException {
        if (this.markByteStack.isEmpty()) {
            return;
        }
        long pos = this.markByteStack.remove(this.markByteStack.size() - 1).longValue();
        if (pos < this.flushedPos) {
            throw new IOException("Previous marked position has been discarded!");
        }
        seek(pos);
        int offset = this.markBitStack.remove(this.markBitStack.size() - 1).intValue();
        setBitOffset(offset);
    }

    /**
     * Promises not to go back before that position.
     *
     * @throws IndexOutOfBoundsException if it is before the current flushed position or after the
     *     position
     */
    public void flushBefore(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        if (pos > getStreamPosition()) {
            throw new IndexOutOfBoundsException("pos > getStreamPosition()!");
        }
        this.flushedPos = pos;
    }

    /** Discards everything before the current position. */
    public void flush() throws IOException {
        flushBefore(getStreamPosition());
    }

    /** How far it was flushed. */
    public long getFlushedPosition() {
        return this.flushedPos;
    }

    /** No; a subclass that keeps things redefines it. */
    public boolean isCached() {
        return false;
    }

    /** No. */
    public boolean isCachedMemory() {
        return false;
    }

    /** No. */
    public boolean isCachedFile() {
        return false;
    }

    /**
     * Closes.
     *
     * <p>It does not close the underlying stream; see {@link ImageInputStream#close}.
     *
     * @throws IOException if it was already closed
     */
    public void close() throws IOException {
        checkClosed();
        this.isClosed = true;
    }

    /**
     * Closes it if nobody did.
     *
     * <p>It survives because the JDK base class declares it and a subclass may be calling
     * {@code super.finalize()}. Finalization is obsolete and nothing should rely on this: an
     * {@code ImageInputStream} is closed by hand.
     */
    @Override
    protected void finalize() throws Throwable {
        if (!this.isClosed) {
            try {
                close();
            } catch (IOException e) {
                // It was already being finalized; there is nobody to report to.
            }
        }
        super.finalize();
    }

    /** Fills the first {@code n} bytes of the scaffolding. */
    private void readFullyInternal(int n) throws IOException {
        readFully(this.byteBuf, 0, n);
    }

    /** The range check the array {@code readFully} share. */
    private static void checkBounds(int off, int len, int length) {
        if (off < 0 || len < 0 || off + len > length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
}
