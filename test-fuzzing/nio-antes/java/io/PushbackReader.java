package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.FilterReader;
import java.io.Reader;

// KajiLibrary's java.io.PushbackReader — PushbackInputStream in characters.
//
// Same mechanism, and it is worth seeing the pair together: the decorator pattern is what
// lets one idea ("un-read what you looked at") be spelled twice, once per stream flavour,
// instead of once per concrete source. A tokenizer over a StringReader and a tokenizer
// over a file reader both just wrap.
//
// The pushback area fills from the END downwards; `pos == buf.length` means empty.
public class PushbackReader extends FilterReader {

    // Private, matching the JDK — the byte-stream twin exposes its buffer for historical
    // reasons only, and there is no reason to repeat that here.
    private char[] buf;
    private int pos;

    public PushbackReader(Reader in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = new char[size];
        this.pos = size;
    }

    public PushbackReader(Reader in) {
        super(in);
        this.buf = new char[1];
        this.pos = 1;
    }

    public int read() throws IOException {
        if (this.pos < this.buf.length) {
            char c = this.buf[this.pos];
            this.pos = this.pos + 1;
            return c;
        }
        return this.in.read();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        int avail = this.buf.length - this.pos;
        int at = off;
        int want = len;
        if (avail > 0) {
            if (want < avail) {
                avail = want;
            }
            System.arraycopy(this.buf, this.pos, cbuf, at, avail);
            this.pos = this.pos + avail;
            at = at + avail;
            want = want - avail;
        }
        if (want > 0) {
            int got = this.in.read(cbuf, at, want);
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

    // Unchecked rather than the JDK's IOException — see PushbackInputStream.unread.
    public void unread(int c) throws IOException {
        if (this.pos == 0) {
            throw new IllegalStateException("pushback buffer overflow");
        }
        this.pos = this.pos - 1;
        this.buf[this.pos] = (char) c;
    }

    public void unread(char[] cbuf, int off, int len) throws IOException {
        if (len > this.pos) {
            throw new IllegalStateException("pushback buffer overflow");
        }
        this.pos = this.pos - len;
        System.arraycopy(cbuf, off, this.buf, this.pos, len);
    }

    public void unread(char[] cbuf) throws IOException {
        this.unread(cbuf, 0, cbuf.length);
    }

    // Anything pushed back is by definition available without blocking; only when the
    // pushback area is empty does the question reach the wrapped Reader.
    public boolean ready() throws IOException {
        if (this.pos < this.buf.length) {
            return true;
        }
        return this.in.ready();
    }

    // mark/reset are revoked here, exactly as in PushbackInputStream: a mark cannot
    // describe a position that is partly inside the pushback area.
    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException("mark/reset not supported");
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException("mark/reset not supported");
    }

    // Routed through read() so that pushed-back characters are skipped before the source
    // is touched — skipping straight on `in` would leave them stranded in front.
    public long skip(long n) throws IOException {
        long remaining = n;
        boolean atEnd = false;
        while (remaining > 0L && !atEnd) {
            if (this.read() < 0) {
                atEnd = true;
            } else {
                remaining = remaining - 1L;
            }
        }
        return n - remaining;
    }

    public void close() throws IOException {
        this.buf = null;
        this.in.close();
    }
}
