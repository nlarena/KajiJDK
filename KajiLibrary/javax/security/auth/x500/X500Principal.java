package javax.security.auth.x500;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.X500Principal -- an X.501 distinguished name.
 *
 * <p>It is how someone is named in a certificate: the subject, the issuer, the holder of a
 * revocation list. A DN is an **ordered sequence** of steps --country, organization, unit, common
 * name-- that go from the general to the particular, and the order matters because the name **is**
 * the path.
 *
 * <h2>The two forms, and why they go the opposite way</h2>
 *
 * <p>The same name is written in two ways and both have to be kept in mind, because the order is
 * **opposite**:
 *
 * <ul>
 *   <li>In **text** (RFC 2253) it goes from the particular to the general:
 *       {@code CN=Juan, OU=Sales, O=Acme, C=AR}.
 *   <li>In **DER** it goes from the general to the particular: first the country, last the common
 *       name.
 * </ul>
 *
 * <p>It is nobody's whim: the DER reflects the hierarchy of the directory --one goes down from the
 * root-- and the text reflects how a name is read aloud. Reversing it is the most common mistake
 * when implementing this class, and it produces certificates that look fine and chain wrong.
 *
 * <h2>The three output formats</h2>
 *
 * <ul>
 *   <li>{@link #RFC2253} -- the standard way of writing a DN. It is the one {@link #getName()}
 *     returns.
 *   <li>{@link #RFC1779} -- the old form, with spaces after the commas and {@code OID.x.y} for the
 *       types that have no keyword.
 *   <li>{@link #CANONICAL} -- the one for **comparing**, not for showing: all in lower case, no
 *       extra spaces, with the inner spaces collapsed. Two DNs that designate the same entity give
 *       the same canonical string even if they were written differently, and that is the only thing
 *       it is for. See {@code AttrValue.canonical} for what the JDK does beyond that and this does
 *       not.
 * </ul>
 *
 * <h2>What is here and what is not</h2>
 *
 * <p>This is **encoding, not cryptography**: parsing a name and writing it again makes no trust
 * decision. A mistake here gives a misread name or an exception, never a signature accepted without
 * verifying. That is why it can be implemented whole and for real, unlike almost everything around
 * it.
 *
 * <p>What is **not** here: the class's own serialization (`writeObject`/`readObject`). The JDK's
 * writes the DER encoding as a {@code byte[]} and reads it back; this one has neither, and its
 * field `rdns` is an array of `Rdn`, which is not `Serializable`, so serializing a principal fails.
 * The note said the library had no `ObjectOutputStream` and that writing the serialization would be
 * inventing a format nobody could read; `java.io.ObjectOutputStream` is there, and it writes the
 * same bytes as the JDK's.
 */
public final class X500Principal implements java.security.Principal, java.io.Serializable {

    /** The old form: spaces after the commas, `OID.x.y` for what has no keyword. */
    public static final String RFC1779 = "RFC1779";

    /** The normal way of writing a DN. It is the one {@link #getName()} returns. */
    public static final String RFC2253 = "RFC2253";

    /**
     * The form for **comparing**: lower case, no extra spaces. It is not for showing to anybody.
     */
    public static final String CANONICAL = "CANONICAL";

    // The steps of the name, in the **text's** order: from the most particular to the most general.
    // It is kept this way and not the other because it is the order in which it is written and
    // read; the DER reverses it on the way out.
    private final Rdn[] rdns;

    // The original DER encoding, when the name came from bytes. It is kept **as is** instead of
    // being re-encoded, and that matters: a certificate is signed over its exact bytes, so
    // returning them re-encoded --even if equivalent-- breaks the verification of the signature.
    private final byte[] originalDer;

    // ---- construction ---------------------------------------------------------------------------

    /**
     * The name written in RFC 2253.
     *
     * @throws IllegalArgumentException if it does not parse
     */
    public X500Principal(String name) {
        this(name, java.util.Collections.<String, String>emptyMap());
    }

    /**
     * The above with keywords of **one's own**.
     *
     * <p>The map goes from keyword to OID, and serves for names that use types the standard did not
     * name. The known keywords still hold; the map's are added. (In the JDK the map's take
     * precedence over the known ones; here a known keyword cannot be redefined.)
     *
     * @throws IllegalArgumentException if it does not parse, or if an OID of the map is malformed
     */
    public X500Principal(String name, Map<String, String> keywordMap) {
        if (name == null || keywordMap == null) {
            throw new NullPointerException();
        }
        this.rdns = Parser.parse(name, keywordMap);
        this.originalDer = null;
    }

    /**
     * The name encoded in DER.
     *
     * @throws IllegalArgumentException if the bytes are not a valid `Name`
     */
    public X500Principal(byte[] name) {
        if (name == null) {
            throw new NullPointerException();
        }
        byte[] copyOf = new byte[name.length];
        int i = 0;
        while (i < copyOf.length) {
            copyOf[i] = name[i];
            i = i + 1;
        }
        try {
            this.rdns = Der.readName(copyOf);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        this.originalDer = copyOf;
    }

    /**
     * The name read from a stream.
     *
     * <p>It reads **only** the name and leaves the stream just after it, which is what allows
     * reading a certificate field by field. To know where it ends, the DER length is looked at, not
     * the end of the stream.
     *
     * @throws IllegalArgumentException if there is no valid `Name` at that position
     */
    public X500Principal(InputStream is) {
        if (is == null) {
            throw new NullPointerException();
        }
        byte[] bytes;
        try {
            bytes = Der.readOneValue(is);
            this.rdns = Der.readName(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        this.originalDer = bytes;
    }

    // ---- output ---------------------------------------------------------------------------------

    /** The name in RFC 2253. */
    public String getName() {
        return this.getName(RFC2253);
    }

    /**
     * The name in the requested format.
     *
     * @throws IllegalArgumentException if the format is none of the three
     */
    public String getName(String format) {
        return this.getName(format, java.util.Collections.<String, String>emptyMap());
    }

    /**
     * The above with OIDs of **one's own** translated to keywords.
     *
     * <p>The map goes the other way from the constructor's: from OID to keyword. It is the same
     * dictionary read in the other direction, and it is separate because one does not always want
     * to write with the same words one read with.
     *
     * @throws IllegalArgumentException if the format is none of the three, or if a non-empty map is
     *         passed with {@link #CANONICAL}, which admits no translations. The JDK rejects {@code
     *         CANONICAL} with any map, even an empty one ("invalid format specified"); here an
     *         empty one gives the canonical form
     */
    public String getName(String format, Map<String, String> oidMap) {
        if (format == null || oidMap == null) {
            throw new NullPointerException();
        }
        if (RFC2253.equalsIgnoreCase(format)) {
            return NameFormat.write(this.rdns, oidMap, false, false);
        }
        if (RFC1779.equalsIgnoreCase(format)) {
            return NameFormat.write(this.rdns, oidMap, true, false);
        }
        if (CANONICAL.equalsIgnoreCase(format)) {
            // Canonical admits no dictionary: if two programs translated differently, two equal
            // names would give different strings and the canonical form would not serve for the
            // only thing it serves for, which is comparing.
            if (!oidMap.isEmpty()) {
                throw new IllegalArgumentException("CANONICAL does not accept an OID map");
            }
            return NameFormat.write(this.rdns, oidMap, false, true);
        }
        throw new IllegalArgumentException("invalid format: " + format);
    }

    /**
     * The name in DER.
     *
     * <p>If it came from bytes, **those** bytes are returned, not a re-encoding: a certificate is
     * signed over its exact encoding, and returning an equivalent but different one breaks the
     * verification.
     */
    public byte[] getEncoded() {
        byte[] source = this.originalDer != null ? this.originalDer : Der.writeName(this.rdns);
        byte[] copyOf = new byte[source.length];
        int i = 0;
        while (i < copyOf.length) {
            copyOf[i] = source[i];
            i = i + 1;
        }
        return copyOf;
    }

    public String toString() {
        return this.getName(RFC1779);
    }

    /**
     * Whether the two names designate the same entity.
     *
     * <p>The **canonical** form is compared, not the bytes: `CN=Juan,O=Acme` and `cn=juan, o=acme`
     * are the same name written differently, and comparing the DER would say they are not.
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof X500Principal)) {
            return false;
        }
        return this.getName(CANONICAL).equals(((X500Principal) o).getName(CANONICAL));
    }

    public int hashCode() {
        return this.getName(CANONICAL).hashCode();
    }

    // ================================================================================================
    // A step of the name: a set of type=value pairs. Almost always just one; more than one is a
    // "multi-valued RDN", which is written with `+` and serves to tell apart two entities with the
    // same name.
    // ================================================================================================

    static final class Rdn {
        final String[] types;   // OID in numeric form, always
        final String[] values;

        Rdn(String[] types, String[] values) {
            this.types = types;
            this.values = values;
        }
    }

    // ================================================================================================
    // The dictionary of keywords. It is the standard's (RFC 4514 and the additions in common use),
    // and it goes in both directions because it is read in both.
    // ================================================================================================

    static final String[][] KNOWN = {
        {"CN", "2.5.4.3"},
        {"L", "2.5.4.7"},
        {"ST", "2.5.4.8"},
        {"O", "2.5.4.10"},
        {"OU", "2.5.4.11"},
        {"C", "2.5.4.6"},
        {"STREET", "2.5.4.9"},
        {"SERIALNUMBER", "2.5.4.5"},
        {"T", "2.5.4.12"},
        {"SURNAME", "2.5.4.4"},
        {"GIVENNAME", "2.5.4.42"},
        {"INITIALS", "2.5.4.43"},
        {"GENERATION", "2.5.4.44"},
        {"DNQUALIFIER", "2.5.4.46"},
        {"DC", "0.9.2342.19200300.100.1.25"},
        {"UID", "0.9.2342.19200300.100.1.1"},
        {"EMAILADDRESS", "1.2.840.113549.1.9.1"},
    };

    static String oidForWord(String word) {
        int i = 0;
        while (i < KNOWN.length) {
            if (KNOWN[i][0].equalsIgnoreCase(word)) {
                return KNOWN[i][1];
            }
            i = i + 1;
        }
        return null;
    }

    static String wordForOid(String oid) {
        int i = 0;
        while (i < KNOWN.length) {
            if (KNOWN[i][1].equals(oid)) {
                return KNOWN[i][0];
            }
            i = i + 1;
        }
        return null;
    }
}
