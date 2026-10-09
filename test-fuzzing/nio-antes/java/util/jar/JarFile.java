package java.util.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A JAR opened for random access: a {@link ZipFile} that also understands the manifest and the
 * per-version entries.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p><b>No signature is verified.</b> The constructors' `verify` parameter is accepted and ignored,
 * and {@link JarEntry#getCertificates()} and {@link JarEntry#getCodeSigners()} always return `null`.
 * Verifying for real asks for PKCS#7, certificate chains and path validation, and none of that
 * exists in this library; writing a half verification would be worse than not having one, because
 * whoever uses it believes something was checked.
 *
 * <p>What matters about that absence is <b>which way it fails</b>, and it fails to the safe side:
 * since the signers are always `null`, a signed JAR looks exactly like an unsigned one, and code
 * that decides trust by looking at the signature **rejects**. What does not happen, and it has to be
 * known, is that a signed and then tampered JAR throws `SecurityException` on being read: here it is
 * read and that is that.
 *
 * <p>Multi-release IS implemented: with the four-argument constructor, `getEntry`/`getJarEntry` look
 * for `META-INF/versions/N/<name>` from the requested version downwards before falling back to the
 * base name, and {@link #versionedStream()} enumerates the resolved view.
 *
 * <h2>Two signature deviations, and the reason</h2>
 *
 * <p><b>The constructors do not declare `throws IOException`.</b> This library's `ZipFile`'s do not
 * either: they wrap the error in an `UncheckedIOException`, and since these delegate to those,
 * declaring the checked exception would promise a signalling that does not happen. The **methods**
 * do declare it, just as `ZipFile`'s do.
 *
 * <p><b>`entries()` returns `Enumeration&lt;ZipEntry&gt;` and `stream()` a
 * `Stream&lt;ZipEntry&gt;`</b>, where the JDK says `JarEntry`. It is not a choice: in the JDK
 * `ZipFile` declares `Enumeration<? extends ZipEntry>`, and the wildcard is precisely what lets the
 * subclass narrow; this library's `ZipFile` declares `Enumeration<ZipEntry>` with no wildcard, so
 * narrowing does not compile. The objects that come out **are** `JarEntry`, and casting them works.
 * Fixing it properly means touching `java.util.zip.ZipFile`, which is another session's.
 */
public class JarFile extends ZipFile {

    /** `META-INF/MANIFEST.MF`, the only name a JAR looks for its manifest under. */
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    private static final String META_INF = "META-INF/";
    private static final String VERSIONS_PREFIX = "META-INF/versions/";

    // The "base" version is 8 and not 1.8 nor 0: it is the one a JAR with no `META-INF/versions/`
    // represents, and the JDK prints it as "8".
    private static final int BASE_FEATURE = 8;
    private static final Runtime.Version BASE_VERSION = Runtime.Version.parse("8");

    private final Runtime.Version version;
    private final int featureVersion;

    private Manifest man;
    private boolean manifestRead;
    // 0 = not looked up, 1 = yes, 2 = no. Looking it up asks for reading the manifest, and that
    // cannot be done in the constructor without changing its error signalling.
    private int multi;

    /** The version a JAR with no versioned entries represents. It is `8`. */
    public static Runtime.Version baseVersion() {
        return BASE_VERSION;
    }

    /** This VM's version, which is the one the constructors that do not ask for it use. */
    public static Runtime.Version runtimeVersion() {
        return Runtime.version();
    }

    public JarFile(String name) {
        this(new File(name), true, ZipFile.OPEN_READ, BASE_VERSION);
    }

    public JarFile(String name, boolean verify) {
        this(new File(name), verify, ZipFile.OPEN_READ, BASE_VERSION);
    }

    public JarFile(File file) {
        this(file, true, ZipFile.OPEN_READ, BASE_VERSION);
    }

    public JarFile(File file, boolean verify) {
        this(file, verify, ZipFile.OPEN_READ, BASE_VERSION);
    }

    public JarFile(File file, boolean verify, int mode) {
        this(file, verify, mode, BASE_VERSION);
    }

    /**
     * It opens the JAR resolving the versioned entries up to `version`.
     *
     * @throws NullPointerException if `version` is `null`
     */
    public JarFile(File file, boolean verify, int mode, Runtime.Version version) {
        super(file, mode);
        if (version == null) {
            throw new NullPointerException("version");
        }
        // Below 9 there are no versioned entries that count, so it is normalised to the base: it is
        // what the JDK does, and it keeps `getVersion()` from lying with a "1.8".
        if (version.feature() < BASE_FEATURE) {
            this.version = BASE_VERSION;
        } else {
            this.version = version;
        }
        this.featureVersion = this.version.feature();
        this.multi = 0;
    }

    /**
     * The version this JAR resolves to, or the base one if it is not multi-release.
     *
     * <p>An ordinary JAR opened asking for version 21 still answers `8`: the version only matters if
     * the manifest declares `Multi-Release: true`.
     */
    public final Runtime.Version getVersion() {
        return isMultiRelease() ? this.version : BASE_VERSION;
    }

    /** Whether the manifest declares `Multi-Release: true`. */
    public final boolean isMultiRelease() {
        if (this.multi == 0) {
            this.multi = 2;
            try {
                Manifest m = getManifest();
                if (m != null) {
                    String v = m.getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE);
                    if (v != null && v.trim().equalsIgnoreCase("true")) {
                        this.multi = 1;
                    }
                }
            } catch (IOException e) {
                // An unreadable manifest is not multi-release. It is not propagated because the JDK
                // declares no exception here either.
                this.multi = 2;
            }
        }
        return this.multi == 1;
    }

    /**
     * The JAR's manifest, or `null` if it has none.
     *
     * <p>It is looked up first by the exact name and then case-insensitively, which is what the JDK
     * does: there are files in circulation with `META-INF/manifest.mf`.
     */
    public Manifest getManifest() throws IOException {
        if (!this.manifestRead) {
            this.manifestRead = true;
            ZipEntry e = manifestEntry();
            if (e != null) {
                InputStream in = super.getInputStream(e);
                if (in != null) {
                    this.man = new Manifest(in);
                    in.close();
                }
            }
        }
        return this.man;
    }

    private ZipEntry manifestEntry() {
        ZipEntry e = super.getEntry(MANIFEST_NAME);
        if (e != null) {
            return e;
        }
        Enumeration<ZipEntry> all = super.entries();
        while (all.hasMoreElements()) {
            ZipEntry z = all.nextElement();
            if (MANIFEST_NAME.equalsIgnoreCase(z.getName())) {
                return z;
            }
        }
        return null;
    }

    /** The entry by that name, resolving by version where appropriate. */
    public JarEntry getJarEntry(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (this.featureVersion > BASE_FEATURE && !name.startsWith(META_INF) && isMultiRelease()) {
            int v = this.featureVersion;
            while (v > BASE_FEATURE) {
                ZipEntry ze = super.getEntry(VERSIONS_PREFIX + v + "/" + name);
                if (ze != null) {
                    return new JarFileEntry(this, ze, name);
                }
                v = v - 1;
            }
        }
        ZipEntry ze = super.getEntry(name);
        if (ze == null) {
            return null;
        }
        return new JarFileEntry(this, ze, name);
    }

    /** The same as {@link #getJarEntry}: in a JAR every entry is a JAR entry. */
    public ZipEntry getEntry(String name) {
        return getJarEntry(name);
    }

    /**
     * Every entry in the file, **without** resolving by version.
     *
     * <p>The objects are `JarEntry`; the static type is `ZipEntry` for the reason the class's header
     * gives. For the resolved view there is {@link #versionedStream()}.
     */
    public Enumeration<ZipEntry> entries() {
        return java.util.Collections.enumeration(rawList());
    }

    /** The same as {@link #entries()}, as a stream. */
    public java.util.stream.Stream<ZipEntry> stream() {
        return rawList().stream();
    }

    /**
     * The **resolved** view of a multi-release JAR: one element per base name, with the bytes of the
     * highest version that does not exceed the one asked for.
     *
     * <p>In a JAR that is not multi-release it is the same as {@link #stream()}.
     */
    public java.util.stream.Stream<ZipEntry> versionedStream() {
        if (!isMultiRelease()) {
            return stream();
        }
        List<String> names = new ArrayList<String>();
        Enumeration<ZipEntry> all = super.entries();
        while (all.hasMoreElements()) {
            String base = baseName(all.nextElement().getName());
            if (base != null && !names.contains(base)) {
                names.add(base);
            }
        }
        List<ZipEntry> out = new ArrayList<ZipEntry>();
        for (String n : names) {
            JarEntry je = getJarEntry(n);
            if (je != null) {
                out.add(je);
            }
        }
        return out.stream();
    }

    /**
     * An entry's base name, or `null` if it is a versioned entry that does not count: the
     * `META-INF/versions/` directory itself, a version's own, or a version higher than the one asked
     * for.
     */
    private String baseName(String name) {
        if (!name.startsWith(VERSIONS_PREFIX)) {
            return name;
        }
        int from = VERSIONS_PREFIX.length();
        int slash = name.indexOf('/', from);
        if (slash < 0 || slash == name.length() - 1) {
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(name.substring(from, slash));
        } catch (NumberFormatException e) {
            // An entry with a "version" that is not a number is ignored in silence, just as in the
            // JDK: it is a badly built file, not a mistake of whoever reads it.
            return null;
        }
        if (v > this.featureVersion) {
            return null;
        }
        return name.substring(slash + 1);
    }

    /**
     * That entry's content.
     *
     * <p>If the entry came resolved by version, what is opened is the **real** name: the
     * `META-INF/versions/` one, not the one the entry says it is called.
     */
    public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
        if (ze == null) {
            throw new NullPointerException("ze");
        }
        if (ze instanceof JarFileEntry) {
            String real = ((JarFileEntry) ze).real();
            if (!real.equals(ze.getName())) {
                ZipEntry z = super.getEntry(real);
                if (z == null) {
                    return null;
                }
                return super.getInputStream(z);
            }
        }
        return super.getInputStream(ze);
    }

    private List<ZipEntry> rawList() {
        List<ZipEntry> out = new ArrayList<ZipEntry>();
        Enumeration<ZipEntry> all = super.entries();
        while (all.hasMoreElements()) {
            ZipEntry z = all.nextElement();
            out.add(new JarFileEntry(this, z, z.getName()));
        }
        return out;
    }
}
