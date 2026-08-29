package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.InputStream;

// KajiLibrary's java.io.ByteArrayInputStream — an InputStream that reads out of a byte[]
// handed to it, tracking a read position. Pure Java: the mirror image of
// ByteArrayOutputStream, and equally runnable on our own code.
public class ByteArrayInputStream extends InputStream {

    protected byte[] buf;
    protected int pos;
    protected int count;

    public ByteArrayInputStream(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    public synchronized int read() {
        if (this.pos < this.count) {
            int b = this.buf[this.pos] & 0xff;
            this.pos = this.pos + 1;
            return b;
        }
        return -1;
    }

    public synchronized int available() {
        return this.count - this.pos;
    }
}
