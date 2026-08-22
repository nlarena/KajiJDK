package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;

// KajiLibrary's java.io.FilterReader — FilterInputStream's character-stream twin: it
// forwards every operation to the Reader it wraps, so a subclass overrides only what it
// changes. The two hierarchies are deliberately parallel; the difference is the unit
// (char, not byte), which is what lets a character decorator count lines or push a
// character back without ever thinking about encodings.
//
// Abstract, unlike FilterOutputStream: a Reader that only forwards has no reason to
// exist, so the class refuses to be instantiated even though it implements everything.
public abstract class FilterReader extends Reader {

    // The wrapped source; a subclass reads through it directly.
    protected Reader in;

    protected FilterReader(Reader in) {
        this.in = in;
    }

    public int read() {
        return this.in.read();
    }

    public int read(char[] cbuf, int off, int len) {
        return this.in.read(cbuf, off, len);
    }

    public long skip(long n) {
        return this.in.skip(n);
    }

    public boolean ready() {
        return this.in.ready();
    }

    public boolean markSupported() {
        return this.in.markSupported();
    }

    public void mark(int readAheadLimit) {
        this.in.mark(readAheadLimit);
    }

    public void reset() {
        this.in.reset();
    }

    public void close() {
        this.in.close();
    }
}
