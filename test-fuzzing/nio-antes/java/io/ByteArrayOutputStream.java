package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.OutputStream;
import java.nio.charset.Charset;

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

    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        this.buf = new byte[size];
        this.count = 0;
    }

    public synchronized void write(int b) {
        this.ensureCapacity(this.count + 1);
        this.buf[this.count] = (byte) b;
        this.count = this.count + 1;
    }

    public synchronized void write(byte[] b, int off, int len) {
        this.ensureCapacity(this.count + len);
        for (int i = 0; i < len; i++) {
            this.buf[this.count + i] = b[off + i];
        }
        this.count = this.count + len;
    }

    public synchronized int size() {
        return this.count;
    }

    public synchronized void reset() {
        this.count = 0;
    }

    // A right-sized copy of the bytes written so far.
    public synchronized byte[] toByteArray() {
        byte[] copy = new byte[this.count];
        System.arraycopy(this.buf, 0, copy, 0, this.count);
        return copy;
    }

    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(this.buf, 0, this.count);
    }

    public void writeBytes(byte[] b) {
        write(b, 0, b.length);
    }

    /** Decodes the bytes with the platform default charset. */
    public synchronized String toString() {
        return new String(this.buf, 0, this.count);
    }

    /** Decodes with the named charset. */
    public synchronized String toString(String charsetName) throws UnsupportedEncodingException {
        return new String(this.buf, 0, this.count, charsetName);
    }

    /** Decodes with the given charset. */
    public synchronized String toString(Charset charset) {
        return new String(this.buf, 0, this.count, charset);
    }

    /**
     * @deprecated Uses {@code hibyte} as the high byte of each character rather than a charset.
     */
    public synchronized String toString(int hibyte) {
        return new String(this.buf, hibyte, 0, this.count);
    }

    /** No resources to release: the buffer is plain memory. */
    public void close() throws IOException {
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
