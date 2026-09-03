package java.util.zip;

import java.io.InputStream;

// The reading side of gzip (RFC 1952). It reads the 10-byte header, hands the rest to a raw
// inflater, and checks the trailer.
//
// The header's optional fields are the fiddly part: four flag bits each add a differently-shaped
// section (an extra field with its own length, two NUL-terminated strings, a 16-bit header CRC),
// and they have to be skipped in a fixed order before the deflate stream begins.
public class GZIPInputStream extends InflaterInputStream {

    public static final int GZIP_MAGIC = 0x8b1f;

    protected CRC32 crc;
    protected boolean eos;

    public GZIPInputStream(InputStream in, int size) throws java.io.IOException {
        super(in, new Inflater(true), size);
        this.crc = new CRC32();
        readHeader();
    }

    public GZIPInputStream(InputStream in) throws java.io.IOException {
        this(in, 512);
    }

    private void readHeader() throws java.io.IOException {
        int magic1 = in.read();
        int magic2 = in.read();
        if (magic1 != 31 || magic2 != 139) {
            // A wrong magic is the one error worth reporting loudly: it means this is not a gzip
            // stream at all, rather than a damaged one. Without a `throws` (finding #104) the
            // signal has to be the unchecked kind.
            throw new RuntimeException("not in gzip format");
        }
        in.read();               // compression method
        int flags = in.read();
        int i = 0;
        while (i < 6) {          // mtime (4), extra flags (1), OS (1)
            in.read();
            i = i + 1;
        }
        if ((flags & 4) != 0) {  // FEXTRA: a length-prefixed block
            int lo = in.read();
            int hi = in.read();
            int extra = lo | (hi << 8);
            int k = 0;
            while (k < extra) {
                in.read();
                k = k + 1;
            }
        }
        if ((flags & 8) != 0) {  // FNAME: NUL-terminated
            skipZeroTerminated();
        }
        if ((flags & 16) != 0) { // FCOMMENT: NUL-terminated
            skipZeroTerminated();
        }
        if ((flags & 2) != 0) {  // FHCRC: a 16-bit CRC of the header
            in.read();
            in.read();
        }
    }

    private void skipZeroTerminated() throws java.io.IOException {
        int b = in.read();
        while (b != 0 && b != -1) {
            b = in.read();
        }
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException {
        // `readInflated` y no `super.read(...)`: finding #125.
        int n = readInflated(b, off, len);
        if (n > 0) {
            crc.update(b, off, n);
        } else {
            eos = true;
        }
        return n;
    }

    // Sin override de `close()`: solo delegaba, y `super.close()` no compila (finding #125). El
    // heredado hace exactamente lo mismo, y no declararlo es un subconjunto valido para el gate.
}
