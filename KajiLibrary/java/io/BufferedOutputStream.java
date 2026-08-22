package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterOutputStream;
import java.io.OutputStream;

// KajiLibrary's java.io.BufferedOutputStream — BufferedInputStream's counterpart on the
// way out: bytes accumulate in an array and reach the sink in one big write.
//
// The asymmetry worth noticing is that buffered output needs `flush()` and buffered input
// does not. Holding bytes back is invisible while the program is running and fatal if the
// program ends — or waits for an answer — without pushing them out. Every "my socket
// hangs" / "my file is truncated" bug in this corner of the library is a missing flush,
// which is why close() (inherited from FilterOutputStream) flushes first.
public class BufferedOutputStream extends FilterOutputStream {

    // Bytes written by the caller but not yet handed to `out`: buf[0..count).
    protected byte[] buf;
    protected int count;

    public BufferedOutputStream(OutputStream out) {
        super(out);
        // Spelled out rather than shared as a `static final int`: finding #112 would make
        // such a constant read back as 0, i.e. a buffer that can never hold anything.
        this.init(8192);
    }

    public BufferedOutputStream(OutputStream out, int size) {
        super(out);
        this.init(size);
    }

    private void init(int size) {
        this.buf = new byte[size];
        this.count = 0;
    }

    public void write(int b) {
        if (this.count >= this.buf.length) {
            this.flushBuffer();
        }
        this.buf[this.count] = (byte) b;
        this.count = this.count + 1;
    }

    public void write(byte[] b, int off, int len) {
        // A write at least as big as the buffer is passed straight through. Copying it in
        // would only mean copying it out again immediately, and — more subtly — it would
        // let one huge write evict a buffer that was usefully filling up.
        if (len >= this.buf.length) {
            this.flushBuffer();
            this.out.write(b, off, len);
        } else {
            if (len > this.buf.length - this.count) {
                this.flushBuffer();
            }
            System.arraycopy(b, off, this.buf, this.count, len);
            this.count = this.count + len;
        }
    }

    // Empty our buffer AND ask the sink to flush: a flush has to travel the whole length
    // of the decorator chain, or an inner buffer would still be sitting on the bytes.
    public void flush() {
        this.flushBuffer();
        this.out.flush();
    }

    private void flushBuffer() {
        if (this.count > 0) {
            this.out.write(this.buf, 0, this.count);
            this.count = 0;
        }
    }
}
