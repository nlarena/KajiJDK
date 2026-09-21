package javax.imageio.stream;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * KajiLibrary's javax.imageio.stream.ImageOutputStreamImpl -- all of {@link ImageOutputStream}
 * except access to the data.
 *
 * <p>The mirror of {@link ImageInputStreamImpl}: a subclass supplies {@link #write(int)} and
 * {@link #write(byte[], int, int)}, and the rest comes from here. And since it extends that one, it
 * also has to supply the two {@code read}.
 *
 * <h2>Pending bits and {@link #flushBits}</h2>
 *
 * <p>Writing loose bits leaves a half-filled byte. This class applies the rule: every write of a
 * byte or more calls {@link #flushBits}, which closes the byte by padding with zeros.
 *
 * <p>{@code flushBits} does something surprising: it <b>reads</b> the byte in the stream before
 * rewriting it, so as not to overwrite the bits already there. It is the reason
 * {@code ImageOutputStream} also has to be readable.
 */
public abstract class ImageOutputStreamImpl extends ImageInputStreamImpl
    implements ImageOutputStream {

    /** For the subclasses. */
    public ImageOutputStreamImpl() {
    }

    /** The subclass has to supply it. */
    public abstract void write(int b) throws IOException;

    /** Writes the array. */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /** The subclass has to supply it. */
    public abstract void write(byte[] b, int off, int len) throws IOException;

    /** One byte: 1 or 0. */
    public void writeBoolean(boolean v) throws IOException {
        if (v) {
            write(1);
        } else {
            write(0);
        }
    }

    /** The low byte. */
    public void writeByte(int v) throws IOException {
        write(v);
    }

    /** Two bytes, in the configured order. */
    public void writeShort(int v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 8);
            this.byteBuf[1] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
        }
        write(this.byteBuf, 0, 2);
    }

    /** Dos bytes. */
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    /** Four bytes. */
    public void writeInt(int v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 24);
            this.byteBuf[1] = (byte) (v >>> 16);
            this.byteBuf[2] = (byte) (v >>> 8);
            this.byteBuf[3] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
            this.byteBuf[2] = (byte) (v >>> 16);
            this.byteBuf[3] = (byte) (v >>> 24);
        }
        write(this.byteBuf, 0, 4);
    }

    /** Eight bytes. */
    public void writeLong(long v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 56);
            this.byteBuf[1] = (byte) (v >>> 48);
            this.byteBuf[2] = (byte) (v >>> 40);
            this.byteBuf[3] = (byte) (v >>> 32);
            this.byteBuf[4] = (byte) (v >>> 24);
            this.byteBuf[5] = (byte) (v >>> 16);
            this.byteBuf[6] = (byte) (v >>> 8);
            this.byteBuf[7] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
            this.byteBuf[2] = (byte) (v >>> 16);
            this.byteBuf[3] = (byte) (v >>> 24);
            this.byteBuf[4] = (byte) (v >>> 32);
            this.byteBuf[5] = (byte) (v >>> 40);
            this.byteBuf[6] = (byte) (v >>> 48);
            this.byteBuf[7] = (byte) (v >>> 56);
        }
        write(this.byteBuf, 0, 8);
    }

    /** Four bytes. */
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /** Eight bytes. */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /** One byte per character; it only works for ASCII. */
    public void writeBytes(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            write((byte) s.charAt(i));
            i = i + 1;
        }
    }

    /** Two bytes per character. */
    public void writeChars(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            writeChar(s.charAt(i));
            i = i + 1;
        }
    }

    /**
     * In modified UTF-8, always in network order.
     *
     * <p>The order is restored even if it fails, as when reading.
     *
     * @throws java.io.UTFDataFormatException if the encoded string is longer than 65535 bytes
     */
    public void writeUTF(String s) throws IOException {
        flushBits();
        int strlen = s.length();
        int utflen = 0;
        int i = 0;
        while (i < strlen) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utflen = utflen + 1;
            } else if (c > 0x07FF) {
                utflen = utflen + 3;
            } else {
                utflen = utflen + 2;
            }
            i = i + 1;
        }
        if (utflen > 65535) {
            throw new java.io.UTFDataFormatException("encoded string too long: "
                + utflen + " bytes");
        }
        byte[] bytearr = new byte[utflen + 2];
        bytearr[0] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[1] = (byte) (utflen & 0xFF);
        int count = 2;
        i = 0;
        while (i < strlen) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                bytearr[count] = (byte) c;
                count = count + 1;
            } else if (c > 0x07FF) {
                bytearr[count] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytearr[count + 2] = (byte) (0x80 | (c & 0x3F));
                count = count + 3;
            } else {
                bytearr[count] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytearr[count + 1] = (byte) (0x80 | (c & 0x3F));
                count = count + 2;
            }
            i = i + 1;
        }
        // The length goes in network order no matter what, which is why the whole array is built by
        // hand instead of using writeShort, which would honour the configured order.
        write(bytearr, 0, utflen + 2);
    }

    /** That part of the array, two bytes per element. */
    public void writeShorts(short[] s, int off, int len) throws IOException {
        checkBounds(off, len, s.length);
        int i = 0;
        while (i < len) {
            writeShort(s[off + i]);
            i = i + 1;
        }
    }

    /** Same, with chars. */
    public void writeChars(char[] c, int off, int len) throws IOException {
        checkBounds(off, len, c.length);
        int i = 0;
        while (i < len) {
            writeChar(c[off + i]);
            i = i + 1;
        }
    }

    /** Same, four bytes. */
    public void writeInts(int[] i, int off, int len) throws IOException {
        checkBounds(off, len, i.length);
        int k = 0;
        while (k < len) {
            writeInt(i[off + k]);
            k = k + 1;
        }
    }

    /** Same, eight bytes. */
    public void writeLongs(long[] l, int off, int len) throws IOException {
        checkBounds(off, len, l.length);
        int i = 0;
        while (i < len) {
            writeLong(l[off + i]);
            i = i + 1;
        }
    }

    /** Same, four-byte floating point. */
    public void writeFloats(float[] f, int off, int len) throws IOException {
        checkBounds(off, len, f.length);
        int i = 0;
        while (i < len) {
            writeFloat(f[off + i]);
            i = i + 1;
        }
    }

    /** Same, eight-byte. */
    public void writeDoubles(double[] d, int off, int len) throws IOException {
        checkBounds(off, len, d.length);
        int i = 0;
        while (i < len) {
            writeDouble(d[off + i]);
            i = i + 1;
        }
    }

    /** One bit; the low bit is taken. */
    public void writeBit(int bit) throws IOException {
        writeBits(bit & 0x1, 1);
    }

    /**
     * The {@code numBits} low bits of the value, from the most significant to the least.
     *
     * <p>It goes one bit at a time, with the same structure as {@code
     * ImageInputStreamImpl.readBit}: read the byte, change the bit that is due, write it back, and
     * go back if the byte is left half filled.
     *
     * <p>It is the simple version. One that gathered whole bytes would be faster and quite a bit
     * easier to break at the edges -- which is where compressed formats live.
     *
     * @throws IllegalArgumentException if more than 64 are asked for
     */
    public void writeBits(long bits, int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException("Bad value for numBits!");
        }
        int i = numBits - 1;
        while (i >= 0) {
            writeSingleBit((int) ((bits >>> i) & 1L));
            i = i - 1;
        }
    }

    /** One bit at the current position, keeping the ones already in that byte. */
    private void writeSingleBit(int bit) throws IOException {
        int offset = this.bitOffset;
        long pos = this.streamPos;
        // The byte is read before rewriting it: otherwise the earlier bits would be lost. That is
        // why an ImageOutputStream also has to be readable.
        seek(pos);
        int existing = read();
        if (existing == -1) {
            existing = 0;
        }
        seek(pos);
        int mask = 1 << (7 - offset);
        int combined;
        if (bit != 0) {
            combined = existing | mask;
        } else {
            combined = existing & ~mask;
        }
        write(combined);
        int next = (offset + 1) & 0x7;
        if (next != 0) {
            // The byte was left half filled: go back over it, and set the offset afterwards
            // because `seek` clears it.
            seek(pos);
        }
        this.bitOffset = next;
    }

    /**
     * Closes the half-written byte, padding with zeros.
     *
     * <p>Every write of a byte or more calls it. See the class note about why it reads before
     * writing.
     */
    protected final void flushBits() throws IOException {
        checkClosed();
        if (this.bitOffset != 0) {
            int offset = this.bitOffset;
            long pos = this.streamPos;
            int partial = read();
            if (partial == -1) {
                partial = 0;
            }
            seek(pos);
            int mask = (0xFF >> offset);
            write(partial & ~mask);
            this.bitOffset = 0;
        }
    }

    /** The range check the {@code writeXs} share. */
    private static void checkBounds(int off, int len, int length) {
        if (off < 0 || len < 0 || off + len > length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
}
