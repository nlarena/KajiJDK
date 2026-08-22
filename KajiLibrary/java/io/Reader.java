package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Closeable;

// KajiLibrary's java.io.Reader — the abstract superclass of character-input streams. A
// subclass supplies the bulk primitive `read(char[], off, len)` and `close`; the single-char
// and full-array reads are layered on it here.
public abstract class Reader implements Closeable {

    // Read up to `len` chars into `cbuf` at `off`; return the count read, or -1 at end.
    public abstract int read(char[] cbuf, int off, int len);

    public abstract void close();

    public int read() {
        char[] one = new char[1];
        if (this.read(one, 0, 1) < 0) {
            return -1;
        }
        return one[0];
    }

    public int read(char[] cbuf) {
        return this.read(cbuf, 0, cbuf.length);
    }

    // Discard up to `n` characters. Unlike a byte stream we cannot just seek forward, since
    // a Reader sits on top of a decoder whose position is only defined by decoding; so the
    // generic way to skip is to read and throw away, which is what this does.
    public long skip(long n) {
        char[] scratch = new char[512];
        long remaining = n;
        boolean atEnd = false;
        while (remaining > 0L && !atEnd) {
            int chunk = 512;
            if (remaining < 512L) {
                chunk = (int) remaining;
            }
            int got = this.read(scratch, 0, chunk);
            if (got < 0) {
                atEnd = true;
            } else {
                remaining = remaining - (long) got;
            }
        }
        return n - remaining;
    }

    // True only if the next read is guaranteed not to block. The conservative answer is
    // false: a source that does not know says "I might block", never the other way round.
    public boolean ready() {
        return false;
    }

    // --- mark / reset ---
    //
    // Same idea as InputStream's, in character units: mark a position, read ahead, reset
    // back. The default is "not supported"; a Reader over an in-memory array or one with a
    // buffer to spare overrides all three. (The JDK's mark/reset throw IOException here;
    // KajiLibrary's java.io is still throws-free — see IOException — so this is unchecked.)
    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) {
        throw new UnsupportedOperationException("mark not supported");
    }

    public void reset() {
        throw new UnsupportedOperationException("reset not supported");
    }
}
