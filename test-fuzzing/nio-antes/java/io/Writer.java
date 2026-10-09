package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.Closeable;
import java.io.Flushable;

// KajiLibrary's java.io.Writer — the abstract superclass of character-output streams. A
// subclass supplies the bulk primitive `write(char[], off, len)` plus `flush`/`close`; the
// single-char, full-array, and String writes are layered on it here.
public abstract class Writer implements Closeable, Flushable, Appendable {

    /**
     * The object this stream **synchronizes** on.
     *
     * <p>It is `protected` and not private because a subclass needs to take it: if `Reader` locked
     * on `this` and the subclass on something else, two threads could get in at once by different
     * routes. Exposing it is what lets the whole hierarchy coordinate on **one** lock.
     *
     * <p>By default it is the stream itself; the one-argument constructor changes it, which is what
     * a decorator uses in order to share the lock with the stream it wraps.
     */
    protected Object lock;

    /** It synchronizes on itself. */
    protected Writer() {
        this.lock = this;
    }

    /**
     * It synchronizes on `lock`.
     *
     * @throws NullPointerException if `lock` is `null` -- a null lock is not "no lock", it is a
     *     failure that turns up much later
     */
    protected Writer(Object lock) {
        if (lock == null) {
            throw new NullPointerException("lock");
        }
        this.lock = lock;
    }

    public abstract void write(char[] cbuf, int off, int len) throws IOException;

    public abstract void flush() throws IOException;

    public abstract void close() throws IOException;

    public void write(int c) throws IOException {
        char[] one = new char[1];
        one[0] = (char) c;
        this.write(one, 0, 1);
    }

    public void write(char[] cbuf) throws IOException {
        this.write(cbuf, 0, cbuf.length);
    }

    public void write(String str) throws IOException {
        int n = str.length();
        char[] cbuf = new char[n];
        for (int i = 0; i < n; i++) {
            cbuf[i] = str.charAt(i);
        }
        this.write(cbuf, 0, n);
    }

    // Write a slice of a String without the caller having to cut a substring first — the
    // point being to avoid allocating a copy of text that is about to be copied again.
    public void write(String str, int off, int len) throws IOException {
        char[] cbuf = new char[len];
        for (int i = 0; i < len; i++) {
            cbuf[i] = str.charAt(off + i);
        }
        this.write(cbuf, 0, len);
    }

    // --- Appendable (each returns this Writer, covariant with Appendable) ---

    public Writer append(char c) throws IOException {
        this.write(c);
        return this;
    }

    public Writer append(CharSequence csq) throws IOException {
        if (csq == null) {
            this.write("null");
        } else {
            this.write(csq.toString());
        }
        return this;
    }

    public Writer append(CharSequence csq, int start, int end) throws IOException {
        if (csq == null) {
            String nul = "null";
            for (int i = start; i < end; i++) {
                this.write(nul.charAt(i));
            }
        } else {
            for (int i = start; i < end; i++) {
                this.write(csq.charAt(i));
            }
        }
        return this;
    }

    /** A writer that discards everything written to it. */
    public static Writer nullWriter() {
        return new NullWriter();
    }

    private static final class NullWriter extends Writer {
        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        public void flush() throws IOException {
        }

        public void close() throws IOException {
        }
    }
}
