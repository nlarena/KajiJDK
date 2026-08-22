package java.util.zip;

import java.io.FilterInputStream;
import java.io.InputStream;

// Compression on the reading side: the caller reads COMPRESSED bytes, and the plain ones are
// pulled from the wrapped stream as needed. Useful when something downstream wants to be handed
// an `InputStream` of compressed data — uploading a file, say — without staging it anywhere.
//
// The `throws IOException` clauses are omitted throughout (finding #104).
public class DeflaterInputStream extends FilterInputStream {

    protected final Deflater def;
    protected final byte[] buf;

    private final boolean ownsDeflater;
    private boolean closed;
    private boolean reachedEnd;

    public DeflaterInputStream(InputStream in) {
        super(in);
        this.def = new Deflater();
        this.buf = new byte[512];
        this.ownsDeflater = true;
    }

    public DeflaterInputStream(InputStream in, Deflater defl) {
        this(in, defl, 512);
    }

    public DeflaterInputStream(InputStream in, Deflater defl, int bufLen) {
        super(in);
        this.def = defl;
        this.buf = new byte[bufLen];
        this.ownsDeflater = false;
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

    public int read(byte[] b, int off, int len) {
        int produced = 0;
        boolean done = false;
        while (produced == 0 && !done) {
            int n = def.deflate(b, off, len);
            if (n > 0) {
                produced = n;
            } else if (def.finished()) {
                done = true;
            } else if (reachedEnd) {
                // Source exhausted: tell the deflater so it can emit its last block and trailer.
                def.finish();
            } else {
                int got = in.read(buf, 0, buf.length);
                if (got == -1) {
                    reachedEnd = true;
                } else {
                    def.setInput(buf, 0, got);
                }
            }
        }
        int result = produced;
        if (produced == 0) {
            result = -1;
        }
        return result;
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

    public int available() {
        int n = 1;
        if (def.finished()) {
            n = 0;
        }
        return n;
    }

    public void close() {
        if (!closed) {
            closed = true;
            if (ownsDeflater) {
                def.end();
            }
            in.close();
        }
    }

    // A mark would have to snapshot the deflater's state, which is not something it can hand out.
    public boolean markSupported() {
        return false;
    }

    public void mark(int readlimit) {
    }

    public void reset() {
    }
}
