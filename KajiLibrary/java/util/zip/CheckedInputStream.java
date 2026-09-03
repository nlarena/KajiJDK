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

    // Sin `throws IOException` a proposito (finding #104): el lector de .class ignora el atributo
    // `Exceptions` del metodo del classpath, asi que ve el override como MAS ANCHO que el original
    // y lo rechaza por 8.4.8.3. La omision es invisible para el gate — `throws` no va en el
    // descriptor — y vuelve cuando se arregle #104.
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
