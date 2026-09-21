package java.util.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A JAR read straight through: a {@link ZipInputStream} that eats the manifest before handing over
 * the first entry.
 *
 * <p>That is the whole difference, and it explains the constructor's one oddity: to know whether
 * there is a manifest the first entry has to be read, and if there is one it has to be consumed
 * whole. When the constructor finishes, the stream has already advanced to the next entry and is
 * holding it; the first `getNextEntry()` returns it.
 *
 * <p>A consequence of that, and it is copied from the JDK on purpose: <b>between the constructor and
 * the first `getNextEntry()`, `read` returns -1</b>. The bytes down there belong to an entry the
 * caller has not asked for yet, and handing them over would be giving them content without saying
 * which entry it is from.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>Nothing of the public surface. As in {@link JarFile}, the `verify` parameter is accepted and
 * ignored --there is no signature verification in this library-- and the entries come out with no
 * certificates and no signers; the full reasoning, including why the failure mode is closed, is in
 * `JarFile`'s header.
 */
public class JarInputStream extends ZipInputStream {

    private Manifest man;
    private JarEntry first;

    public JarInputStream(InputStream in) throws IOException {
        this(in, true);
    }

    public JarInputStream(InputStream in, boolean verify) throws IOException {
        super(in);
        JarEntry e = (JarEntry) super.getNextEntry();
        // A JAR written by `jar` starts with the `META-INF/` directory entry, which is not the
        // manifest but comes before it.
        if (e != null && "META-INF/".equalsIgnoreCase(e.getName())) {
            e = (JarEntry) super.getNextEntry();
        }
        if (e != null && JarFile.MANIFEST_NAME.equalsIgnoreCase(e.getName())) {
            this.man = new Manifest(this);
            super.closeEntry();
            this.first = (JarEntry) super.getNextEntry();
        } else {
            this.first = e;
        }
    }

    /** The JAR's manifest, or `null` if the first entry was not one. */
    public Manifest getManifest() {
        return this.man;
    }

    /** It advances to the next entry, or `null` at the end. */
    public ZipEntry getNextEntry() throws IOException {
        JarEntry e;
        if (this.first == null) {
            e = (JarEntry) super.getNextEntry();
        } else {
            e = this.first;
            this.first = null;
        }
        return e;
    }

    /** The same as {@link #getNextEntry()}, already with this package's type. */
    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry) getNextEntry();
    }

    /** It reads from the current entry. It returns -1 while the first entry has not been asked for. */
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.first != null) {
            return -1;
        }
        return super.read(b, off, len);
    }

    /**
     * The entry the ZIP reader below builds, already with the attributes the manifest assigns it
     * --if the manifest has been read by this point.
     */
    protected ZipEntry createZipEntry(String name) {
        JarEntry e = new JarEntry(name);
        if (this.man != null) {
            e.attr = this.man.getAttributes(name);
        }
        return e;
    }
}
