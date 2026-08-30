package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterInputStream;
import java.io.InputStream;

// KajiLibrary's java.io.DataInputStream — reads Java primitives back from the big-endian binary
// layout {@link DataOutputStream} writes. Strings are "modified UTF-8": a two-byte unsigned length
// then the encoded bytes.
public class DataInputStream extends FilterInputStream implements DataInput {

    public DataInputStream(InputStream in) {
        super(in);
    }

    public final int read(byte[] b) {
        return this.in.read(b, 0, b.length);
    }

    public final int read(byte[] b, int off, int len) {
        return this.in.read(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int count = this.in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n = n + count;
        }
    }

    public final int skipBytes(int n) {
        int total = 0;
        while (total < n) {
            long cur = this.in.skip((long) (n - total));
            if (cur <= 0) {
                break;
            }
            total = total + (int) cur;
        }
        return total;
    }

    public final boolean readBoolean() throws IOException {
        int ch = this.in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch != 0;
    }

    public final byte readByte() throws IOException {
        int ch = this.in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    public final int readUnsignedByte() throws IOException {
        int ch = this.in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    public final short readShort() throws IOException {
        int a = this.in.read();
        int b = this.in.read();
        if ((a | b) < 0) {
            throw new EOFException();
        }
        return (short) ((a << 8) | b);
    }

    public final int readUnsignedShort() throws IOException {
        int a = this.in.read();
        int b = this.in.read();
        if ((a | b) < 0) {
            throw new EOFException();
        }
        return (a << 8) | b;
    }

    public final char readChar() throws IOException {
        int a = this.in.read();
        int b = this.in.read();
        if ((a | b) < 0) {
            throw new EOFException();
        }
        return (char) ((a << 8) | b);
    }

    public final int readInt() throws IOException {
        int a = this.in.read();
        int b = this.in.read();
        int c = this.in.read();
        int d = this.in.read();
        if ((a | b | c | d) < 0) {
            throw new EOFException();
        }
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public final long readLong() throws IOException {
        long result = 0;
        int i = 0;
        while (i < 8) {
            int ch = this.in.read();
            if (ch < 0) {
                throw new EOFException();
            }
            result = (result << 8) | ((long) ch & 0xFF);
            i = i + 1;
        }
        return result;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * @deprecated Reads a line, mapping bytes to chars one for one; does not decode a charset.
     */
    public final String readLine() throws IOException {
        StringBuilder buf = new StringBuilder();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            c = this.in.read();
            if (c < 0 || c == '\n') {
                eol = true;
            } else if (c == '\r') {
                eol = true;
            } else {
                buf.append((char) c);
            }
        }
        if (c < 0 && buf.length() == 0) {
            return null;
        }
        return buf.toString();
    }

    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    public static final String readUTF(DataInput in) throws IOException {
        int utflen = in.readUnsignedShort();
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];
        in.readFully(bytearr, 0, utflen);
        int count = 0;
        int n = 0;
        while (count < utflen) {
            int c = bytearr[count] & 0xFF;
            int shift = c >> 4;
            if (shift <= 7) {
                count = count + 1;
                chararr[n] = (char) c;
                n = n + 1;
            } else if (shift == 12 || shift == 13) {
                count = count + 2;
                if (count > utflen) {
                    throw new UTFDataFormatException("malformed input: partial character at end");
                }
                int c2 = bytearr[count - 1];
                chararr[n] = (char) (((c & 0x1F) << 6) | (c2 & 0x3F));
                n = n + 1;
            } else if (shift == 14) {
                count = count + 3;
                if (count > utflen) {
                    throw new UTFDataFormatException("malformed input: partial character at end");
                }
                int c2 = bytearr[count - 2];
                int c3 = bytearr[count - 1];
                chararr[n] = (char) (((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F));
                n = n + 1;
            } else {
                throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }
        return new String(chararr, 0, n);
    }
}
