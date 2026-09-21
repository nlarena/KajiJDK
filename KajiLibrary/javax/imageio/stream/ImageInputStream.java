package javax.imageio.stream;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * KajiLibrary's javax.imageio.stream.ImageInputStream -- a read stream designed for image formats.
 *
 * <p>It is a {@link DataInput} with three things {@code DataInputStream} lacks, and each answers a
 * real problem of reading images:
 *
 * <ul>
 *   <li><b>configurable byte order</b>. TIFF comes in both orders and says so in its header; BMP
 *       is little-endian, PNG big-endian. With {@code DataInput} every value would have to be
 *       flipped by hand;
 *   <li><b>seeking</b>. {@link #seek} and {@link #getStreamPosition} allow jumping to an offset the
 *       file itself gives -- which is how the whole of TIFF is built;
 *   <li><b>bit reading</b>. {@link #readBit} and {@link #readBits} read fields that are not
 *       byte-aligned, which is normal in compressed formats.
 * </ul>
 *
 * <h2>The flushed position</h2>
 *
 * <p>{@link #flushBefore} promises not to go back before that position, and with that the stream
 * can drop what it had kept. It is what allows reading a huge image from a socket without
 * collecting it whole in memory.
 *
 * <p>The flip side: after that, {@link #seek} to an earlier position throws
 * {@link IndexOutOfBoundsException}. It is not the stream's error but that of whoever promised not
 * to go back.
 *
 * <h2>{@link #mark} stacks</h2>
 *
 * <p>Unlike {@code InputStream}'s, this one is a <b>stack</b>: two {@code mark} in a row and two
 * {@code reset} go back to the second mark and then to the first. And it has no read limit,
 * because the stream can seek.
 *
 * <h2>The bit offset clears itself</h2>
 *
 * <p>Any read of a byte or more sets {@link #getBitOffset} to zero. It is what allows alternating
 * between bit fields and byte fields without keeping count by hand.
 */
public interface ImageInputStream extends DataInput, Closeable {

    /**
     * Which byte order to read multi-byte values with. See the class note.
     *
     * <p>It does not affect {@link #readUTF}, which always reads in network order.
     */
    void setByteOrder(ByteOrder byteOrder);

    /** Which one is set. */
    ByteOrder getByteOrder();

    /**
     * One byte, from 0 to 255, or -1 at the end.
     *
     * @throws IOException if reading failed
     */
    int read() throws IOException;

    /**
     * Until the array is full.
     *
     * @return how many were read, or -1 at the end
     * @throws IOException if reading failed
     */
    int read(byte[] b) throws IOException;

    /**
     * Up to {@code len} bytes.
     *
     * @return how many were read, or -1 at the end
     * @throws IOException if reading failed
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Up to {@code len} bytes, <b>without copying</b>. See {@link IIOByteBuffer}.
     *
     * @throws IOException if reading failed
     */
    void readBytes(IIOByteBuffer buf, int len) throws IOException;

    /**
     * One byte as a boolean.
     *
     * @throws java.io.EOFException if there is no more
     */
    boolean readBoolean() throws IOException;

    /**
     * One signed byte.
     *
     * @throws java.io.EOFException if there is no more
     */
    byte readByte() throws IOException;

    /**
     * One unsigned byte.
     *
     * @throws java.io.EOFException if there is no more
     */
    int readUnsignedByte() throws IOException;

    /**
     * Two signed bytes, in the configured order.
     *
     * @throws java.io.EOFException if there are not enough
     */
    short readShort() throws IOException;

    /**
     * Two unsigned bytes.
     *
     * @throws java.io.EOFException if there are not enough
     */
    int readUnsignedShort() throws IOException;

    /**
     * Two bytes as a char.
     *
     * @throws java.io.EOFException if there are not enough
     */
    char readChar() throws IOException;

    /**
     * Four signed bytes.
     *
     * @throws java.io.EOFException if there are not enough
     */
    int readInt() throws IOException;

    /**
     * Four unsigned bytes, as a {@code long}.
     *
     * <p>It returns {@code long} because a 32-bit unsigned integer does not fit in an {@code int}.
     * It is a type image formats use all the time.
     *
     * @throws java.io.EOFException if there are not enough
     */
    long readUnsignedInt() throws IOException;

    /**
     * Eight bytes.
     *
     * @throws java.io.EOFException if there are not enough
     */
    long readLong() throws IOException;

    /**
     * Four bytes as floating point.
     *
     * @throws java.io.EOFException if there are not enough
     */
    float readFloat() throws IOException;

    /**
     * Eight bytes as floating point.
     *
     * @throws java.io.EOFException if there are not enough
     */
    double readDouble() throws IOException;

    /**
     * A line of text, one byte per character.
     *
     * <p>It inherits the problem of {@code DataInputStream.readLine}: it decodes nothing, so
     * anything that is not ASCII comes out wrong.
     *
     * @return the line, or null at the end of the stream
     */
    String readLine() throws IOException;

    /**
     * A string in modified UTF-8.
     *
     * <p>Always in network order, regardless of {@link #setByteOrder}. It is an old JDK fix: the
     * format defines it that way and honouring the configured order produced unreadable strings.
     *
     * @throws java.io.UTFDataFormatException if the bytes are not valid modified UTF-8
     */
    String readUTF() throws IOException;

    /**
     * Fills that part of the array.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(byte[] b, int off, int len) throws IOException;

    /**
     * Fills the array.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(byte[] b) throws IOException;

    /**
     * Fills that part, two bytes per element and in the configured order.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(short[] s, int off, int len) throws IOException;

    /**
     * Same, with chars.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(char[] c, int off, int len) throws IOException;

    /**
     * Same, four bytes per element.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(int[] i, int off, int len) throws IOException;

    /**
     * Same, eight bytes per element.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(long[] l, int off, int len) throws IOException;

    /**
     * Same, as four-byte floating point.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(float[] f, int off, int len) throws IOException;

    /**
     * Same, eight-byte.
     *
     * @throws java.io.EOFException if there are not enough
     */
    void readFully(double[] d, int off, int len) throws IOException;

    /** At which byte reading is. */
    long getStreamPosition() throws IOException;

    /** At which bit within that byte, from 0 to 7. See the class note. */
    int getBitOffset() throws IOException;

    /**
     * Sets it.
     *
     * @throws IllegalArgumentException if it is not between 0 and 7
     */
    void setBitOffset(int bitOffset) throws IOException;

    /**
     * One bit, 0 or 1. Advances the bit offset.
     *
     * @throws java.io.EOFException if there is no more
     */
    int readBit() throws IOException;

    /**
     * Up to 64 bits, right-aligned in the result.
     *
     * @param numBits from 0 to 64
     * @throws IllegalArgumentException if more than 64 are asked for
     * @throws java.io.EOFException if there are not enough
     */
    long readBits(int numBits) throws IOException;

    /** How many bytes it has, or -1 if unknown. */
    long length() throws IOException;

    /**
     * Skips bytes.
     *
     * @return how many were really skipped
     */
    int skipBytes(int n) throws IOException;

    /** Same, with a skip that may go past two gigabytes. */
    long skipBytes(long n) throws IOException;

    /**
     * Seeks to that byte.
     *
     * @throws IndexOutOfBoundsException if it is before the flushed position; see the class note
     */
    void seek(long pos) throws IOException;

    /** Pushes the current position. See the class note: it stacks. */
    void mark();

    /**
     * Goes back to the last mark.
     *
     * <p>Without a mark it does nothing, as the JDK specifies. (An earlier note said it throws in
     * that case; neither this library's implementation nor the JDK's does.)
     *
     * @throws IOException if the mark lies before the flushed position
     */
    void reset() throws IOException;

    /**
     * Promises not to go back before that position. See the class note.
     *
     * @throws IndexOutOfBoundsException if it is before the current flushed position, or after the
     *     current position
     */
    void flushBefore(long pos) throws IOException;

    /** Discards everything before the current position. */
    void flush() throws IOException;

    /** How far it was flushed. */
    long getFlushedPosition();

    /** Whether it keeps what was read somewhere to be able to go back. */
    boolean isCached();

    /** Whether it keeps it in memory. */
    boolean isCachedMemory();

    /** Whether it keeps it in a temporary file. */
    boolean isCachedFile();

    /**
     * Closes.
     *
     * <p>Whether the underlying object is closed depends on the implementation: the cache streams
     * over an {@code InputStream} or {@code OutputStream} leave it open --whoever opened it closes
     * it, the opposite of almost all of {@code java.io}--, while {@link FileImageInputStream}
     * closes its {@code RandomAccessFile} even when it was handed one. (An earlier note stated the
     * first rule for every implementation.)
     */
    void close() throws IOException;
}
