package java.util.zip;

import java.io.FilterOutputStream;
import java.io.OutputStream;

// The writing counterpart of `CheckedInputStream`: every byte written is folded into the
// checksum on its way out.
public class CheckedOutputStream extends FilterOutputStream {

    private final Checksum checksum;

    public CheckedOutputStream(OutputStream out, Checksum checksum) {
        super(out);
        this.checksum = checksum;
    }

    // This note used to say the `throws IOException` was omitted on purpose because of finding
    // #104 --the class reader ignored a classpath method's `Exceptions` attribute, so it saw the
    // override as WIDER than the original and rejected it by 8.4.8.3. #104 is closed, and the
    // clause is declared below: the note outlived both the defect and the workaround.
    public void write(int b) throws java.io.IOException {
        out.write(b);
        checksum.update(b);
    }

    // Writes the range straight through rather than byte-by-byte. `FilterOutputStream` inherits
    // a loop over `write(int)`, which would be correct but would also checksum through the
    // single-byte path once per byte.
    public void write(byte[] buf, int off, int len) throws java.io.IOException {
        out.write(buf, off, len);
        checksum.update(buf, off, len);
    }

    public Checksum getChecksum() {
        return checksum;
    }
}
