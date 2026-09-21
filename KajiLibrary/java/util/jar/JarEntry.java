package java.util.jar;

import java.io.IOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.zip.ZipEntry;

/**
 * A JAR's entry: a ZIP entry, plus what the manifest and the signature say about it.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>The whole surface is here, but two methods always return `null` and that has to be said here
 * and not in a footnote: <b>{@link #getCertificates()} and {@link #getCodeSigners()} never return
 * anything</b>, because this package does not verify signatures. The reason is in full in
 * {@link JarFile}'s header; what matters from here is that the consequence is **closed**, not open:
 * a signed JAR looks like an unsigned one, so whoever decides trust by looking at the signature
 * rejects instead of accepting.
 *
 * <p>{@link #getRealName()} returns the same name as `getName()` except for the entries `JarFile`
 * resolves by version --the `META-INF/versions/` ones-- which are the only ones in which the two
 * names differ.
 */
public class JarEntry extends ZipEntry {

    Attributes attr;
    Certificate[] certs;
    CodeSigner[] signers;

    /** A fresh entry with that name. */
    public JarEntry(String name) {
        super(name);
    }

    /** A JAR entry with that ZIP entry's data. */
    public JarEntry(ZipEntry ze) {
        super(ze);
    }

    /** A copy. */
    public JarEntry(JarEntry je) {
        super(je);
        this.attr = je.attr;
        this.certs = copyOf(je.certs);
        this.signers = copySigners(je.signers);
    }

    /**
     * The attributes the manifest assigns to this entry, or `null` if it has no section of its own.
     *
     * <p>A loose entry --built with `new JarEntry(name)`-- knows no manifest and returns `null`. The
     * ones that come out of a {@link JarFile} or a {@link JarInputStream} do.
     */
    public Attributes getAttributes() throws IOException {
        return this.attr;
    }

    /** Always `null`: signatures are not verified. See the class's header. */
    public Certificate[] getCertificates() {
        return copyOf(this.certs);
    }

    /** Always `null`: signatures are not verified. See the class's header. */
    public CodeSigner[] getCodeSigners() {
        return copySigners(this.signers);
    }

    /** The entry's real name inside the file. */
    public String getRealName() {
        return getName();
    }

    private static Certificate[] copyOf(Certificate[] a) {
        if (a == null) {
            return null;
        }
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static CodeSigner[] copySigners(CodeSigner[] a) {
        if (a == null) {
            return null;
        }
        CodeSigner[] c = new CodeSigner[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
