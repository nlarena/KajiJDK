package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterInputStream;
import java.io.InputStream;

// KajiLibrary's java.io.BufferedInputStream — the decorator that turns many small reads
// into few large ones.
//
// The reason it is a wrapper and not a feature of each stream: reading one byte at a time
// from a file or a socket costs a system call per byte, while reading 8192 at a time costs
// one per 8192. That arithmetic is the same whatever the source is, so it is written once,
// here, and any InputStream gets it by being wrapped. The stream underneath never learns
// that it happened — it just sees occasional big reads.
//
// Buffering also buys mark/reset for free on a source that cannot rewind: everything from
// the mark is still sitting in the buffer, so resetting is a matter of moving `pos` back.
// That only holds while the marked region fits, which is what `marklimit` is about — the
// caller promises not to read more than that far before resetting, and we promise to keep
// that much.
public class BufferedInputStream extends FilterInputStream {

    // The bytes already pulled from `in` but not yet handed to the caller: valid data is
    // buf[pos..count), so `pos == count` means "empty, go refill".
    protected byte[] buf;
    protected int count;
    protected int pos;

    // Position of the outstanding mark inside buf, or -1 for "no mark". While a mark is
    // live we may not throw the buffer away, since reset() has to find those bytes again.
    protected int markpos;

    // How far past the mark the caller said it would read. Beyond it we are allowed to
    // forget the mark rather than grow the buffer without bound.
    protected int marklimit;

    public BufferedInputStream(InputStream in) {
        super(in);
        // 8192 spelled out rather than shared as a `static final int`: finding #112 makes
        // such a constant read back as 0, i.e. a zero-length buffer.
        this.init(8192);
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in);
        this.init(size);
    }

    private void init(int size) {
        this.buf = new byte[size];
        this.count = 0;
        this.pos = 0;
        this.markpos = -1;
        this.marklimit = 0;
    }

    public int read() {
        if (this.pos >= this.count) {
            this.fill();
            if (this.pos >= this.count) {
                return -1;
            }
        }
        int b = this.buf[this.pos] & 0xff;
        this.pos = this.pos + 1;
        return b;
    }

    public int read(byte[] b, int off, int len) {
        if (len <= 0) {
            return 0;
        }
        // First chunk is mandatory — a read must block until it has at least one byte or
        // end of stream. After that we only keep going while the source says more data is
        // sitting there already: a bulk read is allowed to return short, and returning
        // short beats blocking for bytes the caller may not need.
        int n = this.read1(b, off, len);
        if (n <= 0) {
            return n;
        }
        while (n < len && this.in.available() > 0) {
            int got = this.read1(b, off + n, len - n);
            if (got <= 0) {
                return n;
            }
            n = n + got;
        }
        return n;
    }

    private int read1(byte[] b, int off, int len) {
        int avail = this.count - this.pos;
        if (avail <= 0) {
            // A request at least as big as the buffer, with no mark to preserve, is read
            // straight into the caller's array. Staging it through the buffer would be a
            // pure extra copy — buffering exists to make small reads cheap, not to insert
            // itself into reads that are already large.
            if (len >= this.buf.length && this.markpos < 0) {
                return this.in.read(b, off, len);
            }
            this.fill();
            avail = this.count - this.pos;
            if (avail <= 0) {
                return -1;
            }
        }
        int cnt = avail;
        if (len < cnt) {
            cnt = len;
        }
        System.arraycopy(this.buf, this.pos, b, off, cnt);
        this.pos = this.pos + cnt;
        return cnt;
    }

    // Refill buf from `in`. Everything interesting here is about honouring an outstanding
    // mark: without one the buffer is simply recycled from the top, with one the marked
    // bytes have to survive, by sliding them down, by growing, or — once the caller has
    // read past its own readlimit promise — by dropping the mark.
    private void fill() {
        if (this.markpos < 0) {
            this.pos = 0;
        } else if (this.pos >= this.buf.length) {
            if (this.markpos > 0) {
                // Slide the live region (mark..pos) down to the front; the bytes before
                // the mark can never be asked for again.
                int sz = this.pos - this.markpos;
                System.arraycopy(this.buf, this.markpos, this.buf, 0, sz);
                this.pos = sz;
                this.markpos = 0;
            } else if (this.buf.length >= this.marklimit) {
                // The caller read further than it promised, so the mark is forfeit. This
                // is the escape hatch that stops a forgotten mark from growing the buffer
                // until memory runs out.
                this.markpos = -1;
                this.pos = 0;
            } else {
                int nsz = this.buf.length * 2;
                if (nsz > this.marklimit) {
                    nsz = this.marklimit;
                }
                byte[] nbuf = new byte[nsz];
                System.arraycopy(this.buf, 0, nbuf, 0, this.pos);
                this.buf = nbuf;
            }
        }
        this.count = this.pos;
        int n = this.in.read(this.buf, this.pos, this.buf.length - this.pos);
        if (n > 0) {
            this.count = n + this.pos;
        }
    }

    public long skip(long n) {
        if (n <= 0L) {
            return 0L;
        }
        long avail = (long) (this.count - this.pos);
        if (avail <= 0L) {
            // Nothing buffered and no mark to keep: let the source skip, which it may be
            // able to do without moving any bytes at all (a file just seeks).
            if (this.markpos < 0) {
                return this.in.skip(n);
            }
            this.fill();
            avail = (long) (this.count - this.pos);
            if (avail <= 0L) {
                return 0L;
            }
        }
        long skipped = n;
        if (avail < skipped) {
            skipped = avail;
        }
        this.pos = this.pos + (int) skipped;
        return skipped;
    }

    // What can be read without blocking: what we are holding, plus whatever the source
    // says it has ready.
    public int available() {
        return (this.count - this.pos) + this.in.available();
    }

    public void mark(int readlimit) {
        this.marklimit = readlimit;
        this.markpos = this.pos;
    }

    // Unchecked, like the rest of this package's mark/reset (see InputStream). A failed
    // reset is a programming error — the caller either never marked or broke its own
    // readlimit promise — not an I/O condition.
    public void reset() {
        if (this.markpos < 0) {
            throw new IllegalStateException("resetting to invalid mark");
        }
        this.pos = this.markpos;
    }

    // Always true, even over a source that cannot rewind: the buffer is the rewind.
    public boolean markSupported() {
        return true;
    }

    public void close() {
        this.buf = null;
        this.in.close();
    }
}
