package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.Closeable;
import java.io.Flushable;

// KajiLibrary's java.io.Writer — the abstract superclass of character-output streams. A
// subclass supplies the bulk primitive `write(char[], off, len)` plus `flush`/`close`; the
// single-char, full-array, and String writes are layered on it here.
public abstract class Writer implements Closeable, Flushable, Appendable {

    public abstract void write(char[] cbuf, int off, int len);

    public abstract void flush();

    public abstract void close();

    public void write(int c) {
        char[] one = new char[1];
        one[0] = (char) c;
        this.write(one, 0, 1);
    }

    public void write(char[] cbuf) {
        this.write(cbuf, 0, cbuf.length);
    }

    public void write(String str) {
        int n = str.length();
        char[] cbuf = new char[n];
        for (int i = 0; i < n; i++) {
            cbuf[i] = str.charAt(i);
        }
        this.write(cbuf, 0, n);
    }

    // --- Appendable (each returns this Writer, covariant with Appendable) ---

    public Writer append(char c) {
        this.write(c);
        return this;
    }

    public Writer append(CharSequence csq) {
        if (csq == null) {
            this.write("null");
        } else {
            this.write(csq.toString());
        }
        return this;
    }

    public Writer append(CharSequence csq, int start, int end) {
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
}
