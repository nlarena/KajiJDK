package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.OutputStream;

// KajiLibrary's java.io.FilterOutputStream — the base class of every byte-output
// decorator, and the mirror image of FilterInputStream: it forwards everything to the
// sink it wraps so that a subclass can override just the part it changes.
//
// Unlike the input side this one is public and instantiable, because the JDK made it so
// (PrintStream and friends predate the tidier design); wrapping a stream in a bare
// FilterOutputStream is still a no-op.
public class FilterOutputStream extends OutputStream {

    // The wrapped sink; `protected` for the same reason FilterInputStream.in is.
    protected OutputStream out;

    public FilterOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) throws IOException {
        this.out.write(b);
    }

    // Deliberately one byte at a time through `this.write(int)`, matching the JDK: it
    // guarantees that a subclass which overrides only write(int) really sees every byte.
    // The cost is real — that is precisely why BufferedOutputStream overrides the bulk
    // form, and why wrapping an unbuffered filter in a buffer is the standard move.
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            this.write(b[off + i]);
        }
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    // Flush before closing: a decorator further out may be holding bytes that would
    // otherwise be lost when the sink underneath goes away.
    public void close() throws IOException {
        this.flush();
        this.out.close();
    }
}
