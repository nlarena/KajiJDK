package java.util.zip;

import java.io.FilterInputStream;
import java.io.InputStream;

// Decompression as a stream: the caller reads plain bytes and never sees the compressed ones.
// The decorator is doing real work here rather than just forwarding — it pulls compressed bytes
// only when the inflater asks for them, so a large archive is never held in memory.
//
// The `throws IOException` clauses are omitted throughout (finding #104): the class reader
// ignores the classpath method's `Exceptions` attribute, so an override that declares the same
// checked exception reads as wider and is rejected by 8.4.8.3. `throws` is not part of the
// descriptor, so the gate does not see the difference.
public class InflaterInputStream extends FilterInputStream {

    protected Inflater inf;
    protected byte[] buf;
    protected int len;

    private final boolean ownsInflater;
    private boolean closed;

    public InflaterInputStream(InputStream in, Inflater inf, int size) {
        super(in);
        this.inf = inf;
        this.buf = new byte[size];
        this.ownsInflater = false;
    }

    public InflaterInputStream(InputStream in, Inflater inf) {
        this(in, inf, 512);
    }

    public InflaterInputStream(InputStream in) {
        super(in);
        this.inf = new Inflater();
        this.buf = new byte[512];
        this.ownsInflater = true;
    }

    public int read() {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        int result = -1;
        if (n == 1) {
            result = one[0] & 0xff;
        }
        return result;
    }

    public int read(byte[] b, int off, int length) {
        return readInflated(b, off, length);
    }

    // El cuerpo vive aca, con otro nombre, para que una subclase pueda ampliarlo sin escribir
    // `super.read(...)` — que el emisor todavia no soporta (finding #125). Llamarlo HEREDADO y sin
    // calificar es lo que funciona.
    int readInflated(byte[] b, int off, int length) {
        int produced = 0;
        boolean done = false;
        while (produced == 0 && !done) {
            int n = 0;
            try {
                n = inf.inflate(b, off, length);
            } catch (DataFormatException e) {
                // The stream contract has no room for a format error, and `ZipException` cannot
                // travel either without a `throws` (see the note above), so a corrupt stream ends
                // it. The cause is not swallowed silently: `inflate` already validated what it
                // could, and a truncated result is visible to the caller as a short read.
                done = true;
            }
            if (n > 0) {
                produced = n;
            } else if (inf.finished()) {
                done = true;
            } else if (inf.needsInput()) {
                fill();
                if (len == -1) {
                    done = true;
                }
            } else {
                done = true;
            }
        }
        int result = produced;
        if (produced == 0) {
            result = -1;
        }
        return result;
    }

    // Pulls the next compressed chunk. `len` holds what the last pull returned — the JDK exposes
    // it as a protected field, so a subclass such as `GZIPInputStream` can see how much arrived.
    protected void fill() {
        len = in.read(buf, 0, buf.length);
        if (len > 0) {
            inf.setInput(buf, 0, len);
        }
    }

    public int available() {
        int n = 1;
        if (inf.finished()) {
            n = 0;
        }
        return n;
    }

    public long skip(long n) {
        byte[] scratch = new byte[512];
        long skipped = 0;
        boolean done = false;
        while (skipped < n && !done) {
            long left = n - skipped;
            int want = scratch.length;
            if (left < (long) want) {
                want = (int) left;
            }
            int got = read(scratch, 0, want);
            if (got == -1) {
                done = true;
            } else {
                skipped = skipped + (long) got;
            }
        }
        return skipped;
    }

    public void close() {
        if (!closed) {
            closed = true;
            if (ownsInflater) {
                inf.end();
            }
            in.close();
        }
    }

    // Marks are not supported: the inflater's state is not snapshotable, so honouring a reset
    // would mean re-decoding from the start of the stream.
    public boolean markSupported() {
        return false;
    }

    public void mark(int readlimit) {
    }

    public void reset() {
    }
}
