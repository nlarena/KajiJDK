package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;

// KajiLibrary's java.io.FilterWriter — the character-output decorator base. Same shape as
// the other three filters: forward everything, let a subclass override one thing.
//
// It forwards exactly the three write primitives (char, char[] slice, String slice) and
// the two lifecycle methods. Writer's other overloads — write(char[]), write(String), the
// append() family — are NOT overridden on purpose: they are already implemented in terms
// of the three primitives, so they arrive here through the subclass's override for free.
// Forwarding them as well would give a subclass two different paths to intercept, one of
// which it would inevitably forget.
public abstract class FilterWriter extends Writer {

    // The wrapped sink; a subclass writes through it directly.
    protected Writer out;

    protected FilterWriter(Writer out) {
        this.out = out;
    }

    public void write(int c) throws IOException {
        this.out.write(c);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        this.out.write(cbuf, off, len);
    }

    public void write(String str, int off, int len) throws IOException {
        this.out.write(str, off, len);
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void close() throws IOException {
        this.out.close();
    }
}
