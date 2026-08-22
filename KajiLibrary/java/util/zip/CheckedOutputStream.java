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

    // Sin `throws IOException` a proposito (finding #104): el lector de .class ignora el atributo
    // `Exceptions` del metodo del classpath, asi que ve el override como MAS ANCHO que el original
    // y lo rechaza por 8.4.8.3. La omision es invisible para el gate — `throws` no va en el
    // descriptor — y vuelve cuando se arregle #104.
    public void write(int b) {
        out.write(b);
        checksum.update(b);
    }

    // Writes the range straight through rather than byte-by-byte. `FilterOutputStream` inherits
    // a loop over `write(int)`, which would be correct but would also checksum through the
    // single-byte path once per byte.
    public void write(byte[] buf, int off, int len) {
        out.write(buf, off, len);
        checksum.update(buf, off, len);
    }

    public Checksum getChecksum() {
        return checksum;
    }
}
