package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.Closeable;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;

// KajiLibrary's java.io.InputStream — the abstract superclass of all byte-source streams.
// Subclasses supply the single primitive `read()` (the next byte as 0..255, or -1 at end of
// stream); the bulk `read(byte[])` overloads and `skip` are layered on it here.
public abstract class InputStream implements Closeable {

    // The next byte of data (0..255), or -1 at end of stream. The one operation a concrete
    // source must provide.
    public abstract int read() throws IOException;

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
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
    public long skip(long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            if (this.read() < 0) {
                return n - remaining;
            }
            remaining = remaining - 1;
        }
        return n;
    }

    public int available() throws IOException {
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

    public void reset() throws IOException {
        throw new UnsupportedOperationException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }

    public void close() throws IOException {
    }

    // --- bulk consumption (Java 9+/11+ conveniences) ---

    public byte[] readAllBytes() throws IOException {
        return readNBytes(Integer.MAX_VALUE);
    }

    public byte[] readNBytes(int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] chunk = new byte[len < 8192 ? len : 8192];
        int remaining = len;
        while (remaining > 0) {
            int want = remaining < chunk.length ? remaining : chunk.length;
            int n = read(chunk, 0, want);
            if (n <= 0) {
                break;
            }
            bos.write(chunk, 0, n);
            remaining = remaining - n;
        }
        return bos.toByteArray();
    }

    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int n = read(b, off + total, len - total);
            if (n <= 0) {
                break;
            }
            total = total + n;
        }
        return total;
    }

    public long transferTo(OutputStream out) throws IOException {
        long total = 0;
        byte[] chunk = new byte[8192];
        while (true) {
            int n = read(chunk, 0, chunk.length);
            if (n <= 0) {
                break;
            }
            out.write(chunk, 0, n);
            total = total + n;
        }
        return total;
    }

    public void skipNBytes(long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = skip(remaining);
            if (skipped == 0) {
                if (read() < 0) {
                    throw new EOFException();
                }
                remaining = remaining - 1;
            } else {
                remaining = remaining - skipped;
            }
        }
    }

    /** A stream that is always at end of stream. */
    public static InputStream nullInputStream() {
        return new NullInputStream();
    }

    private static final class NullInputStream extends InputStream {
        public int read() throws IOException {
            return -1;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return len == 0 ? 0 : -1;
        }
    }
}
