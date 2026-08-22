package java.util.zip;

import java.io.FilterOutputStream;
import java.io.OutputStream;

// Compression as a stream: the caller writes plain bytes and the compressed ones go out the
// other side. The asymmetry with `InflaterInputStream` is worth noticing — a decompressor can
// always make progress from whatever it has, but a compressor is holding back data it might
// still describe more cheaply, which is why `finish()` exists at all and why forgetting to close
// one of these truncates the output.
//
// The `throws IOException` clauses are omitted throughout (finding #104) — see
// `InflaterInputStream` for the reason and why the gate does not see it.
public class DeflaterOutputStream extends FilterOutputStream {

    protected Deflater def;
    protected byte[] buf;

    private final boolean ownsDeflater;
    private final boolean syncFlush;
    private boolean closed;

    public DeflaterOutputStream(OutputStream out, Deflater def, int size, boolean syncFlush) {
        super(out);
        this.def = def;
        this.buf = new byte[size];
        this.syncFlush = syncFlush;
        this.ownsDeflater = false;
    }

    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        this(out, def, size, false);
    }

    public DeflaterOutputStream(OutputStream out, Deflater def, boolean syncFlush) {
        this(out, def, 512, syncFlush);
    }

    public DeflaterOutputStream(OutputStream out, Deflater def) {
        this(out, def, 512, false);
    }

    public DeflaterOutputStream(OutputStream out, boolean syncFlush) {
        super(out);
        this.def = new Deflater();
        this.buf = new byte[512];
        this.syncFlush = syncFlush;
        this.ownsDeflater = true;
    }

    public DeflaterOutputStream(OutputStream out) {
        this(out, false);
    }

    public void write(int b) {
        byte[] one = new byte[1];
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    public void write(byte[] b, int off, int len) {
        def.setInput(b, off, len);
        deflate();
    }

    // Drains whatever the deflater is willing to hand over right now. Called after every write
    // and repeatedly from `finish()`; the difference between the two is only whether the
    // deflater has been told there is no more input.
    protected void deflate() {
        int n = def.deflate(buf, 0, buf.length);
        while (n > 0) {
            out.write(buf, 0, n);
            n = def.deflate(buf, 0, buf.length);
        }
    }

    // Ends the compressed stream without closing the underlying one — what a zip writer needs
    // between entries, since each entry is its own compressed stream inside one file.
    public void finish() {
        if (!def.finished()) {
            def.finish();
            deflate();
        }
    }

    public void close() {
        if (!closed) {
            closed = true;
            finish();
            if (ownsDeflater) {
                def.end();
            }
            out.close();
        }
    }

    public void flush() {
        // With `syncFlush` the caller wants what has been written so far to be readable now. Our
        // deflater emits stored blocks, which are already self-contained and byte-aligned, so
        // draining is all a flush point needs to be.
        if (syncFlush) {
            deflate();
        }
        out.flush();
    }
}
