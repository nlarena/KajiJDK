package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterOutputStream;
import java.io.OutputStream;

// KajiLibrary's java.io.DataOutputStream — writes Java primitives to an underlying stream in a
// portable, big-endian binary form, the exact byte layout {@link DataInputStream} reads back.
// Strings go out as "modified UTF-8": a two-byte unsigned length followed by the encoded bytes.
public class DataOutputStream extends FilterOutputStream implements DataOutput {

    protected int written;

    public DataOutputStream(OutputStream out) {
        super(out);
        this.written = 0;
    }

    public synchronized void write(int b) {
        this.out.write(b);
        this.written = this.written + 1;
    }

    public synchronized void write(byte[] b, int off, int len) {
        this.out.write(b, off, len);
        this.written = this.written + len;
    }

    public void flush() {
        this.out.flush();
    }

    public final void writeBoolean(boolean v) {
        write(v ? 1 : 0);
    }

    public final void writeByte(int v) {
        write(v);
    }

    public final void writeShort(int v) {
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }

    public final void writeChar(int v) {
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }

    public final void writeInt(int v) {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }

    public final void writeLong(long v) {
        write((int) (v >>> 56) & 0xFF);
        write((int) (v >>> 48) & 0xFF);
        write((int) (v >>> 40) & 0xFF);
        write((int) (v >>> 32) & 0xFF);
        write((int) (v >>> 24) & 0xFF);
        write((int) (v >>> 16) & 0xFF);
        write((int) (v >>> 8) & 0xFF);
        write((int) v & 0xFF);
    }

    public final void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    public final void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    public final void writeBytes(String s) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            write((int) s.charAt(i) & 0xFF);
            i = i + 1;
        }
    }

    public final void writeChars(String s) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            writeChar(s.charAt(i));
            i = i + 1;
        }
    }

    public final void writeUTF(String s) throws IOException {
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
            throw new UTFDataFormatException("encoded string too long: " + utflen + " bytes");
        }
        write((utflen >>> 8) & 0xFF);
        write(utflen & 0xFF);
        i = 0;
        while (i < strlen) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                write(c);
            } else if (c > 0x07FF) {
                write(0xE0 | ((c >> 12) & 0x0F));
                write(0x80 | ((c >> 6) & 0x3F));
                write(0x80 | (c & 0x3F));
            } else {
                write(0xC0 | ((c >> 6) & 0x1F));
                write(0x80 | (c & 0x3F));
            }
            i = i + 1;
        }
    }

    public final int size() {
        return this.written;
    }
}
