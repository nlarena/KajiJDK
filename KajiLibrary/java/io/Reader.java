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
}
