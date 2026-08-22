package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;

// KajiLibrary's java.io.CharArrayReader — a Reader whose source is a char[].
//
// Not a decorator itself: it is one of the ENDS a chain of decorators is anchored to, the
// character twin of ByteArrayInputStream. That is what makes the decorators testable —
// a LineNumberReader over a CharArrayReader exercises the counting logic with no file, no
// encoding and no I/O anywhere in sight.
//
// Being backed by an array it can rewind for free, so unlike most sources it implements
// mark/reset for real: the mark is just an index.
public class CharArrayReader extends Reader {

    // Readable characters are buf[pos..count); `markedPos` is where reset() goes back to.
    protected char[] buf;
    protected int pos;
    protected int markedPos;
    protected int count;

    public CharArrayReader(char[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
        this.markedPos = 0;
    }

    // A window onto part of an array, so a caller holding one big buffer can hand out a
    // Reader over a slice of it without copying the slice out first.
    public CharArrayReader(char[] buf, int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        int end = offset + length;
        if (end > buf.length) {
            end = buf.length;
        }
        this.count = end;
        this.markedPos = offset;
    }

    public int read() {
        if (this.pos >= this.count) {
            return -1;
        }
        char c = this.buf[this.pos];
        this.pos = this.pos + 1;
        return c;
    }

    public int read(char[] cbuf, int off, int len) {
        if (this.pos >= this.count) {
            return -1;
        }
        if (len <= 0) {
            return 0;
        }
        int n = len;
        if (this.count - this.pos < n) {
            n = this.count - this.pos;
        }
        System.arraycopy(this.buf, this.pos, cbuf, off, n);
        this.pos = this.pos + n;
        return n;
    }

    // An array source can skip by arithmetic — no reading and discarding, which is what
    // Reader.skip has to do when it cannot see where it is.
    public long skip(long n) {
        long avail = (long) (this.count - this.pos);
        long skipped = n;
        if (skipped > avail) {
            skipped = avail;
        }
        if (skipped < 0L) {
            skipped = 0L;
        }
        this.pos = this.pos + (int) skipped;
        return skipped;
    }

    // Always true: the characters are already in memory, so no read can ever block.
    public boolean ready() {
        return true;
    }

    public boolean markSupported() {
        return true;
    }

    // The read-ahead limit is ignored on purpose: it exists so a buffering reader knows
    // how much to retain, and here the whole array is retained regardless.
    public void mark(int readAheadLimit) {
        this.markedPos = this.pos;
    }

    public void reset() {
        this.pos = this.markedPos;
    }

    // Dropping the array is the whole of closing: it releases the reference and makes any
    // later read return end-of-stream rather than silently carrying on.
    public void close() {
        this.buf = null;
        this.pos = 0;
        this.count = 0;
    }
}
