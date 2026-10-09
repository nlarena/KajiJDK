package javax.security.auth.kerberos;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KajiLibrary's javax.security.auth.kerberos.KeyTab -- a file of service keys.
 *
 * <p>A keytab keeps the long-term keys of one or more principals, so that a service authenticates
 * without a password. This class is a <b>reference</b> to the file: it does not read it when
 * created but each time keys are asked for, so it reflects the file as it is at that moment.
 *
 * <h2>Bound or not</h2>
 *
 * <p>A bound keytab ({@link #isBound}) is for one principal: {@link
 * #getInstance(KerberosPrincipal)} binds it to that one, and {@link #getInstance()} binds it "to
 * somebody", the principal resolved at the moment of use. An unbound one --{@link
 * #getUnboundInstance}-- serves anybody. The distinction exists so that a login module knows whom
 * the file represents.
 *
 * <h2>The default file</h2>
 *
 * <p>It comes from the {@code java.security.krb5.keytab} property if it is set, and otherwise it is
 * {@code krb5.keytab} in the user's directory. KajiJDK does not read {@code krb5.conf}.
 *
 * <h2>The format</h2>
 *
 * <p>Version 0x0502 is read --MIT's and Heimdal's since the nineties--: two version bytes and then
 * entries, each with its length, the principal by components, the timestamp, the key version and
 * the key with its type. A file that does not exist or cannot be read gives zero keys, not an
 * error: it is what the JDK does, and what a service without a keytab needs to be able to say "I
 * have no keys" instead of falling over.
 */
public final class KeyTab {

    /** The format version understood. */
    private static final int KEYTAB_VERSION = 0x0502;

    /** The file, or null for the default one. */
    private final File file;

    /** Whom it is bound to, or null if it is bound "to somebody" or not bound. */
    private final KerberosPrincipal princ;

    /** Whether it is bound. See the class note. */
    private final boolean bound;

    /** It is reached through the {@code getInstance}s. */
    private KeyTab(KerberosPrincipal princ, File file, boolean bound) {
        this.princ = princ;
        this.file = file;
        this.bound = bound;
    }

    /**
     * That file, bound to somebody.
     *
     * @throws NullPointerException if the file is null
     */
    public static KeyTab getInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, true);
    }

    /**
     * That file, unbound.
     *
     * @throws NullPointerException if the file is null
     */
    public static KeyTab getUnboundInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, false);
    }

    /**
     * That file, bound to that principal.
     *
     * @throws NullPointerException if either is null
     */
    public static KeyTab getInstance(KerberosPrincipal princ, File file) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(princ, file, true);
    }

    /** The default file, bound to somebody. */
    public static KeyTab getInstance() {
        return new KeyTab(null, null, true);
    }

    /** The default file, unbound. */
    public static KeyTab getUnboundInstance() {
        return new KeyTab(null, null, false);
    }

    /**
     * The default file, bound to that principal.
     *
     * @throws NullPointerException if it is null
     */
    public static KeyTab getInstance(KerberosPrincipal princ) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        return new KeyTab(princ, null, true);
    }

    /**
     * The keys of that principal in the file, in the file's order.
     *
     * <p>Empty if the file does not exist or cannot be read. See the class note.
     *
     * @throws NullPointerException if the principal is null
     */
    public KerberosKey[] getKeys(KerberosPrincipal principal) {
        String wanted = principal.getName();
        List<KerberosKey> keys = new ArrayList<KerberosKey>();
        File source = resolveFile();
        if (source.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(source);
                readEntries(new DataInputStream(in), principal, wanted, keys);
            } catch (IOException e) {
                // A truncated file or one of another version: what was read up to there is what
                // there is.
            } catch (RuntimeException e) {
                // Likewise for a negative length or a component that does not fit.
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // There is nothing left to read.
                    }
                }
            }
        }
        return keys.toArray(new KerberosKey[keys.size()]);
    }

    /** Walks the entries and keeps the ones of the requested principal. See the class note. */
    private static void readEntries(DataInputStream in, KerberosPrincipal principal, String wanted,
                                    List<KerberosKey> keys) throws IOException {
        if (in.readUnsignedShort() != KEYTAB_VERSION) {
            return;
        }
        while (true) {
            int size;
            try {
                size = in.readInt();
            } catch (IOException e) {
                return;
            }
            if (size == 0) {
                return;
            }
            if (size < 0) {
                // A deleted entry: the length is negative and its absolute value has to be skipped.
                skipFully(in, -size);
                continue;
            }
            byte[] entry = new byte[size];
            in.readFully(entry);
            KerberosKey key = parseEntry(new DataInputStream(
                new java.io.ByteArrayInputStream(entry)), principal);
            if (key != null && key.getPrincipal().getName().equals(wanted)) {
                keys.add(key);
            }
        }
    }

    /** An entry, or null if the principal could not be put together. */
    private static KerberosKey parseEntry(DataInputStream in, KerberosPrincipal wanted)
            throws IOException {
        int components = in.readUnsignedShort();
        String realm = readString(in);
        StringBuilder name = new StringBuilder();
        int i = 0;
        while (i < components) {
            if (i > 0) {
                name.append('/');
            }
            name.append(readString(in));
            i = i + 1;
        }
        int nameType = in.readInt();
        in.readInt();
        int versionNumber = in.readUnsignedByte();
        int keyType = in.readUnsignedShort();
        int keyLength = in.readUnsignedShort();
        byte[] keyBytes = new byte[keyLength];
        in.readFully(keyBytes);
        if (in.available() >= 4) {
            // The 32-bit version, optional at the end, overrides the one-byte one when present.
            int longVersion = in.readInt();
            if (longVersion != 0) {
                versionNumber = longVersion;
            }
        }
        KerberosPrincipal principal;
        try {
            principal = new KerberosPrincipal(name + "@" + realm,
                nameType >= 0 && nameType <= 5 || nameType == 10 ? nameType
                    : KerberosPrincipal.KRB_NT_PRINCIPAL);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new KerberosKey(principal, keyBytes, keyType, versionNumber);
    }

    /** A string with its length in front. */
    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    /** Skips those bytes, all of them. */
    private static void skipFully(DataInputStream in, int count) throws IOException {
        int remaining = count;
        while (remaining > 0) {
            int skipped = in.skipBytes(remaining);
            if (skipped <= 0) {
                throw new IOException("keytab truncado");
            }
            remaining = remaining - skipped;
        }
    }

    /** Whether the file exists. */
    public boolean exists() {
        return resolveFile().isFile();
    }

    /** The file that corresponds: the given one, or the default one. See the class note. */
    private File resolveFile() {
        if (this.file != null) {
            return this.file;
        }
        String configured = System.getProperty("java.security.krb5.keytab");
        if (configured != null && !configured.isEmpty()) {
            return new File(configured);
        }
        return new File(System.getProperty("user.home", "."), "krb5.keytab");
    }

    /** The file and whom it is bound to. */
    @Override
    public String toString() {
        String source = this.file == null ? "Default keytab" : this.file.toString();
        if (!this.bound) {
            return source;
        }
        return source + " for " + (this.princ == null ? "someone" : this.princ.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.file, this.princ, this.bound);
    }

    /** Equal if they are the same file --as named, not as resolved-- and the same binding. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeyTab)) {
            return false;
        }
        KeyTab other = (KeyTab) obj;
        return Objects.equals(this.file, other.file) && Objects.equals(this.princ, other.princ)
            && this.bound == other.bound;
    }

    /** Whom it is bound to; null if it is bound "to somebody" or not bound. */
    public KerberosPrincipal getPrincipal() {
        return this.princ;
    }

    /** Whether it is bound. See the class note. */
    public boolean isBound() {
        return this.bound;
    }
}
