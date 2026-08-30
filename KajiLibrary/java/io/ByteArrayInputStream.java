package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.InputStream;
import java.io.OutputStream;

// KajiLibrary's java.io.ByteArrayInputStream — an InputStream that reads out of a byte[]
// handed to it, tracking a read position. Pure Java: the mirror image of
// ByteArrayOutputStream, and equally runnable on our own code.
public class ByteArrayInputStream extends InputStream {

    protected byte[] buf;
    protected int pos;
    protected int mark;
    protected int count;

    public ByteArrayInputStream(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.mark = 0;
        this.count = buf.length;
    }

    public ByteArrayInputStream(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.mark = offset;
        int end = offset + length;
        this.count = end < buf.length ? end : buf.length;
    }

    public synchronized int read() {
        if (this.pos < this.count) {
            int b = this.buf[this.pos] & 0xff;
            this.pos = this.pos + 1;
            return b;
        }
        return -1;
    }

    public synchronized int read(byte[] b, int off, int len) {
        if (this.pos >= this.count) {
            return -1;
        }
        int avail = this.count - this.pos;
        int n = len < avail ? len : avail;
        if (n <= 0) {
            return 0;
        }
        System.arraycopy(this.buf, this.pos, b, off, n);
        this.pos = this.pos + n;
        return n;
    }

    public synchronized byte[] readAllBytes() {
        int n = this.count - this.pos;
        byte[] result = new byte[n];
        System.arraycopy(this.buf, this.pos, result, 0, n);
        this.pos = this.count;
        return result;
    }

    public int readNBytes(byte[] b, int off, int len) {
        int n = read(b, off, len);
        return n < 0 ? 0 : n;
    }

    public synchronized long transferTo(OutputStream out) throws IOException {
        int n = this.count - this.pos;
        out.write(this.buf, this.pos, n);
        this.pos = this.count;
        return n;
    }

    public synchronized long skip(long n) {
        long avail = this.count - this.pos;
        long k = n < avail ? n : avail;
        if (k < 0) {
            k = 0;
        }
        this.pos = this.pos + (int) k;
        return k;
    }

    public synchronized int available() {
        return this.count - this.pos;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readAheadLimit) {
        this.mark = this.pos;
    }

    public synchronized void reset() {
        this.pos = this.mark;
    }

    public void close() {
    }
}
