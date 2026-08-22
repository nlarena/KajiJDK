package java.util.zip;

import java.io.FilterOutputStream;
import java.io.OutputStream;

// The fourth corner of the compression square: `InflaterInputStream` decompresses what you read,
// `DeflaterOutputStream` compresses what you write, `DeflaterInputStream` compresses what you
// read, and this one decompresses what you write. All four wrap the same two engines; which of
// them a caller needs depends only on which side of the pipe holds the compressed bytes.
//
// The `throws IOException` clauses are omitted throughout (finding #104).
public class InflaterOutputStream extends FilterOutputStream {

    protected final Inflater inf;
    protected final byte[] buf;

    private final boolean ownsInflater;
    private boolean closed;

    public InflaterOutputStream(OutputStream out) {
        super(out);
        this.inf = new Inflater();
        this.buf = new byte[512];
        this.ownsInflater = true;
    }

    public InflaterOutputStream(OutputStream out, Inflater infl) {
        this(out, infl, 512);
    }

    public InflaterOutputStream(OutputStream out, Inflater infl, int bufLen) {
        super(out);
        this.inf = infl;
        this.buf = new byte[bufLen];
        this.ownsInflater = false;
    }

    public void write(int b) {
        byte[] one = new byte[1];
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    public void write(byte[] b, int off, int len) {
        inf.setInput(b, off, len);
        drain();
    }

    // Pushes out whatever the inflater can produce from what it has been given.
    private void drain() {
        boolean more = true;
        while (more) {
            int n = 0;
            try {
                n = inf.inflate(buf, 0, buf.length);
            } catch (DataFormatException e) {
                more = false;
            }
            if (n > 0) {
                out.write(buf, 0, n);
            } else {
                more = false;
            }
        }
    }

    public void finish() {
        drain();
    }

    public void flush() {
        drain();
        out.flush();
    }

    public void close() {
        if (!closed) {
            closed = true;
            finish();
            if (ownsInflater) {
                inf.end();
            }
            out.close();
        }
    }
}
