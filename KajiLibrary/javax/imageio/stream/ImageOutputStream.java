package javax.imageio.stream;

import java.io.DataOutput;
import java.io.IOException;

/**
 * KajiLibrary's javax.imageio.stream.ImageOutputStream -- a write stream for image formats.
 *
 * <p>It extends {@link ImageInputStream} and not just {@link DataOutput}, and that draws attention:
 * a <b>write</b> stream that can also be read.
 *
 * <p>It is not an oversight. Writing an image format almost always needs to go back: a header is
 * written with a length not yet known, the image is written, and the header is corrected. Without
 * being able to read and seek, that forces building the whole file in memory.
 *
 * <p>It inherits the configurable byte order, and applies it when writing.
 *
 * <h2>Pending bits</h2>
 *
 * <p>{@link #writeBit} and {@link #writeBits} leave bits in a half-filled byte. Any write of a byte
 * or more <b>closes</b> that byte by padding with zeros, just as reading clears the offset.
 *
 * <p>The half-filled byte is already in the stream: each bit write reads the byte, sets the bit and
 * writes it back. So closing the stream with pending bits does not lose them; the byte goes out
 * with its remaining bits at zero. (An earlier note said they are discarded unless something else
 * is written or {@link #flush} is called. Checked on the JDK 25 and on this VM: three bits written
 * and closed come out as one byte, 160 for {@code 101}.)
 */
public interface ImageOutputStream extends ImageInputStream, DataOutput {

    /**
     * Writes the low byte.
     *
     * @throws IOException if it failed
     */
    void write(int b) throws IOException;

    /**
     * Writes the array.
     *
     * @throws IOException if it failed
     */
    void write(byte[] b) throws IOException;

    /**
     * Writes that part of the array.
     *
     * @throws IOException if it failed
     */
    void write(byte[] b, int off, int len) throws IOException;

    /** One byte: 1 or 0. */
    void writeBoolean(boolean v) throws IOException;

    /** The low byte. */
    void writeByte(int v) throws IOException;

    /** Two bytes, in the configured order. */
    void writeShort(int v) throws IOException;

    /** Dos bytes. */
    void writeChar(int v) throws IOException;

    /** Four bytes. */
    void writeInt(int v) throws IOException;

    /** Eight bytes. */
    void writeLong(long v) throws IOException;

    /** Four bytes. */
    void writeFloat(float v) throws IOException;

    /** Eight bytes. */
    void writeDouble(double v) throws IOException;

    /**
     * One byte per character.
     *
     * <p>The high byte of each is lost; it only works for ASCII.
     */
    void writeBytes(String s) throws IOException;

    /** Two bytes per character, in the configured order. */
    void writeChars(String s) throws IOException;

    /**
     * In modified UTF-8.
     *
     * <p>Always in network order, like {@link ImageInputStream#readUTF}.
     *
     * @throws java.io.UTFDataFormatException if the encoded string is longer than 65535 bytes
     */
    void writeUTF(String s) throws IOException;

    /** That part of the array, two bytes per element. */
    void writeShorts(short[] s, int off, int len) throws IOException;

    /** Same, with chars. */
    void writeChars(char[] c, int off, int len) throws IOException;

    /** Same, four bytes per element. */
    void writeInts(int[] i, int off, int len) throws IOException;

    /** Same, eight bytes. */
    void writeLongs(long[] l, int off, int len) throws IOException;

    /** Same, four-byte floating point. */
    void writeFloats(float[] f, int off, int len) throws IOException;

    /** Same, eight-byte. */
    void writeDoubles(double[] d, int off, int len) throws IOException;

    /**
     * One bit; the low bit of the argument is taken. See the class note.
     *
     * @throws IOException if it failed
     */
    void writeBit(int bit) throws IOException;

    /**
     * The {@code numBits} low bits of the value.
     *
     * @param numBits from 0 to 64
     * @throws IllegalArgumentException if more than 64 are asked for
     */
    void writeBits(long bits, int numBits) throws IOException;

    /**
     * Really writes everything before that position and promises not to go back before it.
     *
     * @throws IndexOutOfBoundsException if it is before the flushed position, or after the current
     *     one
     */
    void flushBefore(long pos) throws IOException;
}
