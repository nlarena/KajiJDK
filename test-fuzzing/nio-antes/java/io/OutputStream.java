package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.Closeable;
import java.io.Flushable;

// KajiLibrary's java.io.OutputStream — the abstract superclass of all byte-sink streams.
// Subclasses supply the single primitive `write(int)` (one byte); the bulk `write(byte[])`
// overloads and the no-op `flush`/`close` are layered on it here.
public abstract class OutputStream implements Closeable, Flushable {

    // Write the low 8 bits of `b`. The one operation a concrete sink must provide.
    public abstract void write(int b) throws IOException;

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            this.write(b[off + i]);
        }
    }

    // No-ops by default; a buffered or resource-backed sink overrides them.
    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }

    /** A sink that discards every byte. */
    public static OutputStream nullOutputStream() {
        return new NullOutputStream();
    }

    private static final class NullOutputStream extends OutputStream {
        public void write(int b) throws IOException {
        }

        public void write(byte[] b, int off, int len) throws IOException {
        }
    }
}
