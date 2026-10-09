package java.util.zip;

import java.io.FilterInputStream;
import java.io.InputStream;

// A stream that checksums what passes through it. The decorator earns its keep here: the
// checksum is computed from the bytes the caller actually reads, so nothing has to be buffered
// and nothing has to be read twice — which is exactly how a zip verifies an entry while
// extracting it.
public class CheckedInputStream extends FilterInputStream {

    private final Checksum checksum;

    public CheckedInputStream(InputStream in, Checksum checksum) {
        super(in);
        this.checksum = checksum;
    }

    // This note used to say the `throws IOException` was omitted on purpose because of finding
    // #104 --the class reader ignored a classpath method's `Exceptions` attribute, so it saw the
    // override as WIDER than the original and rejected it by 8.4.8.3. #104 is closed, and the
    // clause is declared below: the note outlived both the defect and the workaround.
    public int read() throws java.io.IOException {
        int b = in.read();
        if (b != -1) {
            checksum.update(b);
        }
        return b;
    }

    public int read(byte[] buf, int off, int len) throws java.io.IOException {
        int n = in.read(buf, off, len);
        if (n != -1) {
            checksum.update(buf, off, n);
        }
        return n;
    }

    // Skipped bytes still count: they are part of the stream, so they must reach the checksum.
    // That forces an actual read — there is no way to checksum a byte without seeing it, which
    // is why this cannot simply delegate to the underlying `skip`.
    public long skip(long n) throws java.io.IOException {
        byte[] buf = new byte[512];
        long skipped = 0;
        while (skipped < n) {
            long left = n - skipped;
            int want = buf.length;
            if (left < (long) want) {
                want = (int) left;
            }
            int got = read(buf, 0, want);
            if (got == -1) {
                skipped = n;
            } else {
                skipped = skipped + (long) got;
            }
        }
        return skipped;
    }

    public Checksum getChecksum() {
        return checksum;
    }
}
