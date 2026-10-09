package java.util.jar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A JAR written straight through: a {@link ZipOutputStream} that starts by putting the manifest in.
 *
 * <p>The order is not a detail. The manifest has to be the file's first entry so that a sequential
 * reader --{@link JarInputStream}, which can only go forwards-- finds it without having read
 * everything else. That is why the constructor that receives it writes it right there.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>Nothing of the public surface. There IS one byte-level difference from the JDK worth noting:
 * the JDK puts a four-byte `extra` field with the magic number `0xCAFE` on the first entry. Not here,
 * because this library's `ZipOutputStream` does not write `extra` fields at all --it writes a length
 * of 0 in the local header-- so setting it would be writing it into an object nobody then
 * serialises. It is not needed for anything: no tool demands it, and the JARs that
 * this stream writes are read by the real `java`.
 */
public class JarOutputStream extends ZipOutputStream {

    public JarOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    /**
     * It writes the manifest as the first entry and leaves the stream ready for the rest.
     *
     * @throws NullPointerException if `man` is `null`
     */
    public JarOutputStream(OutputStream out, Manifest man) throws IOException {
        super(out);
        if (man == null) {
            throw new NullPointerException("man");
        }
        putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
        man.write(this);
        closeEntry();
    }

    /** It starts a fresh entry. */
    public void putNextEntry(ZipEntry ze) throws IOException {
        super.putNextEntry(ze);
    }
}
