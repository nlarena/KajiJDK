package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.InputStream;

// KajiLibrary's java.io.FilterInputStream — the base class of every byte-input decorator.
//
// On its own it does nothing: each method forwards to the stream it wraps. That is the
// whole point. Buffering, pushback and line counting are each ONE behaviour, and they are
// orthogonal to where the bytes come from. Baked into the sources they would multiply —
// a buffered file stream, a buffered socket stream, a pushback file stream, a buffered
// pushback socket stream — so instead each is written once as a wrapper over the
// InputStream abstraction, and they compose by nesting:
//
//     new PushbackInputStream(new BufferedInputStream(anyInputStream))
//
// A concrete decorator then overrides only the methods whose behaviour it actually
// changes and inherits the forwarding for the rest, which is why the do-nothing
// forwarding has to exist as a class at all.
public class FilterInputStream extends InputStream {

    // The wrapped source. It is `protected` rather than private because that is how a
    // decorator reaches the byte it needs: `in.read()`, an ordinary call on a field, not
    // an up-call to an inherited implementation.
    protected InputStream in;

    // Protected: a plain FilterInputStream is indistinguishable from the stream it wraps,
    // so there is no reason to instantiate one — only to extend it.
    protected FilterInputStream(InputStream in) {
        this.in = in;
    }

    public int read() throws IOException {
        return this.in.read();
    }

    // Note this goes through `this.read(b, 0, b.length)` and NOT through `in.read(b)`:
    // the virtual call lands on the subclass's bulk read, so a decorator that overrides
    // only read(byte[],int,int) still sees every full-array read pass through it. Routing
    // it to the wrapped stream instead would silently bypass the decoration.
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return this.in.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return this.in.skip(n);
    }

    public int available() throws IOException {
        return this.in.available();
    }

    public void close() throws IOException {
        this.in.close();
    }

    // mark/reset forward too, so wrapping a rewindable source keeps it rewindable — a
    // decorator must not quietly take a capability away from the stream underneath it.
    public void mark(int readlimit) {
        this.in.mark(readlimit);
    }

    public void reset() throws IOException {
        this.in.reset();
    }

    public boolean markSupported() {
        return this.in.markSupported();
    }
}
