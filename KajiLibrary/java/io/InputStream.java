package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Closeable;

// KajiLibrary's java.io.InputStream — the abstract superclass of all byte-source streams.
// Subclasses supply the single primitive `read()` (the next byte as 0..255, or -1 at end of
// stream); the bulk `read(byte[])` overloads and `skip` are layered on it here.
public abstract class InputStream implements Closeable {

    // The next byte of data (0..255), or -1 at end of stream. The one operation a concrete
    // source must provide.
    public abstract int read();

    public int read(byte[] b) {
        return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) {
        int i = 0;
        while (i < len) {
            int c = this.read();
            if (c < 0) {
                if (i == 0) {
                    return -1;
                }
                return i;
            }
            b[off + i] = (byte) c;
            i = i + 1;
        }
        return i;
    }

    // Discard up to `n` bytes, returning how many were actually skipped.
    public long skip(long n) {
        long remaining = n;
        while (remaining > 0) {
            if (this.read() < 0) {
                return n - remaining;
            }
            remaining = remaining - 1;
        }
        return n;
    }

    public int available() {
        return 0;
    }

    // --- mark / reset ---
    //
    // A caller that needs to look ahead and then un-look (a parser deciding which branch to
    // take) marks a position, reads on, and resets back to it. This lives on InputStream
    // rather than in a separate interface so that ANY stream can be asked; markSupported()
    // is how a source that cannot rewind answers honestly, and the defaults below are
    // exactly that answer. `readlimit` is the caller's promise about how far it will read
    // before resetting, which is what lets a buffered stream size its safety margin.
    //
    // Note the JDK's reset() throws IOException here; the whole of KajiLibrary's java.io is
    // still throws-free (see IOException), so this signals with an unchecked exception.
    public void mark(int readlimit) {
    }

    public void reset() {
        throw new UnsupportedOperationException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }

    public void close() {
    }
}
