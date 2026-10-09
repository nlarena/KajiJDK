package java.util.jar;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * The entry a {@link JarFile} returns: it knows which file it came out of, and that is why it can
 * answer `getAttributes()` by looking at the manifest instead of having to be handed them from
 * outside.
 *
 * <p>It is also the only entry in which `getName()` and `getRealName()` differ. That happens with
 * **multi-release** JARs: whoever asks for `foo/Bar.class` in a JAR opened at version 21 may receive
 * the bytes of `META-INF/versions/21/foo/Bar.class`. The name asked for is the one the entry says it
 * is called --otherwise no `equals` on names would close-- and the one inside the file is left in
 * `getRealName()`.
 *
 * <p>In the JDK this is `JarFile.JarFileEntry`, an inner class. Here it is package-private and
 * top-level, with the reference to the `JarFile` explicit: it is internal, that is, free by the
 * contract rule, and it avoids depending on non-static inner classes.
 */
final class JarFileEntry extends JarEntry {

    private final JarFile owner;
    private final String real;

    JarFileEntry(JarFile owner, ZipEntry ze, String name) {
        super(name);
        this.owner = owner;
        this.real = ze.getName();
        // The entry is named after whoever asked for it but **measures** what it measures inside the file.
        setMethod(ze.getMethod());
        setSize(ze.getSize());
        setCompressedSize(ze.getCompressedSize());
        setTime(ze.getTime());
        setCrc(ze.getCrc());
        setExtra(ze.getExtra());
        setComment(ze.getComment());
    }

    String real() {
        return this.real;
    }

    public String getRealName() {
        return this.real;
    }

    /** The manifest's section for this entry, looked up by its **real** name. */
    public Attributes getAttributes() throws IOException {
        Manifest man = this.owner.getManifest();
        if (man == null) {
            return null;
        }
        return man.getAttributes(this.real);
    }
}
