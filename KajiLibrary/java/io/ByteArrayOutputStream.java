package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.OutputStream;

// KajiLibrary's java.io.ByteArrayOutputStream — an OutputStream that collects everything
// written into a growable in-memory byte[] (doubling when full), retrievable via
// toByteArray(). Pure Java: no VM intrinsics, so it runs entirely on our own code.
public class ByteArrayOutputStream extends OutputStream {

    protected byte[] buf;
    protected int count;

    public ByteArrayOutputStream() {
        this.buf = new byte[32];
        this.count = 0;
    }

    public void write(int b) {
        this.ensureCapacity(this.count + 1);
        this.buf[this.count] = (byte) b;
        this.count = this.count + 1;
    }

    public void write(byte[] b, int off, int len) {
        this.ensureCapacity(this.count + len);
        for (int i = 0; i < len; i++) {
            this.buf[this.count + i] = b[off + i];
        }
        this.count = this.count + len;
    }

    public int size() {
        return this.count;
    }

    public void reset() {
        this.count = 0;
    }

    // A right-sized copy of the bytes written so far.
    public byte[] toByteArray() {
        byte[] copy = new byte[this.count];
        System.arraycopy(this.buf, 0, copy, 0, this.count);
        return copy;
    }

    private void ensureCapacity(int min) {
        if (min > this.buf.length) {
            int newCap = this.buf.length * 2;
            if (newCap < min) {
                newCap = min;
            }
            byte[] grown = new byte[newCap];
            System.arraycopy(this.buf, 0, grown, 0, this.count);
            this.buf = grown;
        }
    }
}
