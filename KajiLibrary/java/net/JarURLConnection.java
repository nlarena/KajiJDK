package java.net;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The connection to a {@code jar:} URL, which names a file inside a `.jar`.
 *
 * <p>The URL's shape is {@code jar:<jar url>!/<entry>}, and the {@code !/} separator is what splits
 * it: what comes before is an ordinary URL --usually {@code file:} or {@code http:}-- and what comes
 * after, the path inside the archive. The entry may be missing, and then the URL names the whole
 * `.jar`: {@code jar:file:/x/a.jar!/} is valid and its {@link #getEntryName} is `null`.
 *
 * <h2>Why this class is abstract and yet does almost everything</h2>
 *
 * <p>The only thing it does not know how to do is **get hold of the file**: that depends on the inner
 * protocol --a {@code file:} is opened, an {@code http:} is downloaded and cached-- and that is why
 * {@link #getJarFile} is left to the subclass. Everything else can be written once and here it is:
 * splitting the URL, finding the entry, reading the manifest, getting the attributes and the
 * certificates. It is the JDK's division of labour and it is the right one -- eight of the ten
 * members do not depend on the transport.
 *
 * <h2>The attributes: two methods people confuse</h2>
 *
 * <p>{@link #getAttributes} are **the entry's** and {@link #getMainAttributes} are **the manifest's
 * main section's**, which hold for the whole archive. A signed `.jar` keeps each file's digest in its
 * own section, and there the difference stops being academic.
 */
public abstract class JarURLConnection extends URLConnection {

    /**
     * The connection to the `.jar`'s own URL.
     *
     * <p>It is `null` until a subclass opens it. It is declared here and `protected` because the JDK
     * does so: it is the point where a subclass passes options --timeouts, headers-- to the inner
     * connection.
     */
    protected URLConnection jarFileURLConnection;

    private final URL jarFileURL;
    private final String entryName;

    /**
     * Splits the URL into the `.jar` and the entry.
     *
     * @throws MalformedURLException if it is not of the form {@code jar:<url>!/<entry>}
     */
    protected JarURLConnection(URL url) throws MalformedURLException {
        super(url);
        String spec = url.getFile();
        int sep = spec.indexOf("!/");
        if (sep == -1) {
            // The `!/` is not decorative: without it there is no telling where the inner URL ends,
            // and a `jar:` URL with no separator names nothing. The JDK throws in exactly this
            // place.
            throw new MalformedURLException("no !/ in spec");
        }
        this.jarFileURL = new URL(spec.substring(0, sep));
        String rest = spec.substring(sep + 2);
        // An empty entry means "the whole archive", and that is `null` and not `""`: the contract
        // tells the two apart, and a `""` would read as an entry with an empty name.
        this.entryName = rest.isEmpty() ? null : JarURLConnection.decode(rest);
    }

    // The entry's name comes percent-encoded, like any part of a URL. Undecoded, a file with a space
    // in its name is looked up as `a%20b.txt` and is never found.
    private static String decode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    sb.append((char) ((hi << 4) | lo));
                    i = i + 3;
                    continue;
                }
            }
            sb.append(c);
            i = i + 1;
        }
        return sb.toString();
    }

    /** The `.jar`'s URL, without the entry part. */
    public URL getJarFileURL() {
        return this.jarFileURL;
    }

    /** The entry's name, or `null` if the URL names the whole archive. */
    public String getEntryName() {
        return this.entryName;
    }

    /**
     * The opened `.jar`.
     *
     * <p>The one thing this class cannot do on its own: it depends on the inner protocol. See the
     * class's note.
     *
     * @throws IOException if it could not be opened
     */
    public abstract JarFile getJarFile() throws IOException;

    /**
     * The `.jar`'s manifest, or `null` if it has none.
     *
     * <p>A `.jar` with no manifest is perfectly valid --it is a zip-- so `null` is not an error but
     * the answer.
     *
     * @throws IOException if the archive could not be opened
     */
    public Manifest getManifest() throws IOException {
        return this.getJarFile().getManifest();
    }

    /**
     * The entry the URL names, or `null` if it names the whole archive **or if it does not exist**.
     *
     * <p>Both cases give `null`, and that reads badly but it is the contract: this class does not
     * check that the entry exists. The one that does is the concrete protocol handler, on connecting,
     * and that is why a real `jar:` fails with `FileNotFoundException` long before getting here.
     *
     * <p>I wrote this method throwing for a missing entry --it seemed better than returning `null`--
     * and the JDK corrected me: the behaviour test did not match when run against the real `java`.
     * Telling the two `null`s apart is the subclass's job, not this one's.
     *
     * @throws IOException if the archive could not be opened
     */
    public JarEntry getJarEntry() throws IOException {
        if (this.entryName == null) {
            return null;
        }
        return this.getJarFile().getJarEntry(this.entryName);
    }

    /**
     * **The entry's** attributes, or `null` if the URL names the whole archive.
     *
     * <p>See the class's note on the difference from {@link #getMainAttributes}.
     *
     * @throws IOException if the archive could not be opened, or if the entry does not exist
     */
    public Attributes getAttributes() throws IOException {
        JarEntry e = this.getJarEntry();
        return e == null ? null : e.getAttributes();
    }

    /**
     * The manifest's main section's attributes, or `null` if there is no manifest.
     *
     * @throws IOException if the archive could not be opened
     */
    public Attributes getMainAttributes() throws IOException {
        Manifest m = this.getManifest();
        return m == null ? null : m.getMainAttributes();
    }

    /**
     * The certificates the entry was signed with, or `null`.
     *
     * <p><strong>They only hold after reading the whole entry</strong>, and that is not a detail of
     * this implementation but how signing a `.jar` works: the digest is checked while the bytes are
     * read, so asking beforehand returns `null` even if the archive is signed. The JDK says the same.
     *
     * @throws IOException if the archive could not be opened, or if the entry does not exist
     */
    public Certificate[] getCertificates() throws IOException {
        JarEntry e = this.getJarEntry();
        return e == null ? null : e.getCertificates();
    }
}
