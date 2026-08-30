package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;

// KajiLibrary's java.io.PushbackInputStream — the decorator that lets a byte be put back.
//
// It exists for parsers. Deciding what a token is almost always means reading one byte too
// many — you know the number ended because a non-digit turned up — and the reader that
// consumed that byte is not the one that should interpret it. Pushback makes "give it
// back" a one-liner, and because it is a decorator, any InputStream gets it, including
// sources that have no notion of a position to rewind to.
//
// The pushed-back bytes live in `buf` and are filled from the END downwards: `pos` is the
// index of the most recently unread byte, so `pos == buf.length` means the pushback area
// is empty. That way a read just walks `pos` upwards and pops bytes in the reverse order
// they were unread, which is the order the caller pushed them expecting to see them again.
public class PushbackInputStream extends FilterInputStream {

    // The pushback area; valid (unread) bytes are buf[pos..buf.length).
    protected byte[] buf;
    protected int pos;

    public PushbackInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = new byte[size];
        this.pos = size;
    }

    // One byte of pushback is the common case: a parser that looked ahead by one.
    public PushbackInputStream(InputStream in) {
        super(in);
        this.buf = new byte[1];
        this.pos = 1;
    }

    public int read() {
        if (this.pos < this.buf.length) {
            int b = this.buf[this.pos] & 0xff;
            this.pos = this.pos + 1;
            return b;
        }
        return this.in.read();
    }

    public int read(byte[] b, int off, int len) {
        if (len <= 0) {
            return 0;
        }
        // Drain the pushback area first, then top up from the source — never the other
        // way round, or the bytes would come back out in the wrong order.
        int avail = this.buf.length - this.pos;
        int at = off;
        int want = len;
        if (avail > 0) {
            if (want < avail) {
                avail = want;
            }
            System.arraycopy(this.buf, this.pos, b, at, avail);
            this.pos = this.pos + avail;
            at = at + avail;
            want = want - avail;
        }
        if (want > 0) {
            int got = this.in.read(b, at, want);
            if (got < 0) {
                if (avail == 0) {
                    return -1;
                }
                return avail;
            }
            return avail + got;
        }
        return avail;
    }

    // Unchecked, unlike the JDK's IOException: this package is throws-free (see
    // IOException), and overrunning the pushback area is a programming error anyway —
    // the caller asked for a buffer of n bytes and pushed back more than n.
    public void unread(int b) {
        if (this.pos == 0) {
            throw new IllegalStateException("push back buffer is full");
        }
        this.pos = this.pos - 1;
        this.buf[this.pos] = (byte) b;
    }

    public void unread(byte[] b, int off, int len) {
        if (len > this.pos) {
            throw new IllegalStateException("push back buffer is full");
        }
        // Copied as a block so that b[off..off+len) reads back in its original order:
        // unreading an array is "I consumed these, take them back", not len separate pushes.
        this.pos = this.pos - len;
        System.arraycopy(b, off, this.buf, this.pos, len);
    }

    public void unread(byte[] b) {
        this.unread(b, 0, b.length);
    }

    public int available() {
        return (this.buf.length - this.pos) + this.in.available();
    }

    public long skip(long n) {
        if (n <= 0L) {
            return 0L;
        }
        long skipped = (long) (this.buf.length - this.pos);
        long want = n;
        if (skipped > 0L) {
            if (want < skipped) {
                skipped = want;
            }
            this.pos = this.pos + (int) skipped;
            want = want - skipped;
        }
        if (want > 0L) {
            skipped = skipped + this.in.skip(want);
        }
        return skipped;
    }

    // Pushback and mark/reset cannot both be honest here: a mark taken while bytes were
    // pushed back would have to remember pushback state the source knows nothing about.
    // So this decorator REVOKES the capability rather than forwarding it — the one thing
    // a filter is allowed to take away, and it says so instead of quietly getting it wrong.
    public boolean markSupported() {
        return false;
    }

    public void mark(int readlimit) {
    }

    public void reset() {
        throw new UnsupportedOperationException("mark/reset not supported");
    }

    public synchronized void close() {
        this.buf = null;
        this.in.close();
    }

    public long transferTo(OutputStream out) throws IOException {
        long total = 0;
        byte[] chunk = new byte[8192];
        while (true) {
            int n = read(chunk, 0, chunk.length);
            if (n <= 0) {
                break;
            }
            out.write(chunk, 0, n);
            total = total + n;
        }
        return total;
    }
}
