package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;

/**
 * A named mapping between sequences of characters and sequences of bytes.
 *
 * <p>The class exists because "text" and "bytes" are not the same thing and the conversion
 * between them is neither obvious nor unique. Every charset here has a canonical name, zero or
 * more aliases, and the ability to hand out a fresh {@link CharsetEncoder} or {@link
 * CharsetDecoder} that does the actual work.
 *
 * <p>Instances are immutable and safe to share across threads; the coders they produce are
 * neither, which is exactly why they are produced rather than kept.
 *
 * <p>Names are matched without regard to case, so {@code "utf-8"} and {@code "UTF-8"} find the
 * same charset, but the canonical name that comes back from {@link #name} preserves the
 * registered spelling.
 */
public abstract class Charset implements Comparable<Charset> {

    private final String canonicalName;
    private final String[] aliasNames;

    /**
     * Initialises a charset with its canonical name and aliases.
     *
     * @param canonicalName the canonical name; must be a legal charset name
     * @param aliases other names this charset answers to, or null for none
     * @throws IllegalCharsetNameException if the canonical name or any alias is not legal
     */
    protected Charset(String canonicalName, String[] aliases) {
        Charset.checkName(canonicalName);
        String[] copy = aliases == null ? new String[0] : new String[aliases.length];
        int i = 0;
        while (i < copy.length) {
            Charset.checkName(aliases[i]);
            copy[i] = aliases[i];
            i = i + 1;
        }
        this.canonicalName = canonicalName;
        this.aliasNames = copy;
    }

    // The syntax rules for a charset name, from RFC 2278: letters, digits, and the five
    // punctuation marks, with the punctuation forbidden in first position. Enforced here rather
    // than left to the lookup so that an illegal name and an unknown one stay distinguishable --
    // they are different exceptions, and the difference is the difference between a typo in the
    // program and a name the program was handed.
    private static void checkName(String name) {
        if (name == null) {
            throw new IllegalCharsetNameException(name);
        }
        int n = name.length();
        if (n == 0) {
            throw new IllegalCharsetNameException(name);
        }
        int i = 0;
        while (i < n) {
            char c = name.charAt(i);
            boolean alnum = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9');
            boolean punct = c == '-' || c == '+' || c == ':' || c == '_' || c == '.';
            if (!alnum && !(punct && i != 0)) {
                throw new IllegalCharsetNameException(name);
            }
            i = i + 1;
        }
    }

    /**
     * Whether a charset with this name is available.
     *
     * @param charsetName a canonical name or an alias
     * @return true if {@link #forName} would succeed
     * @throws IllegalCharsetNameException if the name is not legally shaped
     * @throws IllegalArgumentException if {@code charsetName} is null
     */
    public static boolean isSupported(String charsetName) {
        if (charsetName == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        Charset.checkName(charsetName);
        Charset found = CharsetRegistry.lookup(charsetName);
        return found != null;
    }

    /**
     * The charset with this name.
     *
     * @param charsetName a canonical name or an alias, matched without regard to case
     * @return the charset
     * @throws IllegalCharsetNameException if the name is not legally shaped
     * @throws UnsupportedCharsetException if the name is legal but no such charset is available
     * @throws IllegalArgumentException if {@code charsetName} is null
     */
    public static Charset forName(String charsetName) {
        if (charsetName == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        Charset.checkName(charsetName);
        Charset found = CharsetRegistry.lookup(charsetName);
        if (found == null) {
            throw new UnsupportedCharsetException(charsetName);
        }
        return found;
    }

    /**
     * The charset with this name, or {@code fallback} if there is none.
     *
     * <p>The forgiving form: a name that is unknown OR malformed yields the fallback instead of
     * an exception, which is what a caller wants when the name came from a configuration file
     * and a default is perfectly acceptable.
     *
     * @param charsetName a canonical name or an alias
     * @param fallback what to return when the name does not resolve; may be null
     * @return the named charset, or {@code fallback}
     * @throws IllegalArgumentException if {@code charsetName} is null
     */
    public static Charset forName(String charsetName, Charset fallback) {
        if (charsetName == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        Charset found = CharsetRegistry.lookupUnchecked(charsetName);
        return found == null ? fallback : found;
    }

    /**
     * Every available charset, by canonical name, sorted without regard to case.
     *
     * <p>The returned map is unmodifiable.
     */
    public static SortedMap<String, Charset> availableCharsets() {
        return CharsetRegistry.available();
    }

    /**
     * The default charset, which is UTF-8.
     *
     * <p>A constant here, and a constant in the JDK since 18. It used to follow the platform
     * locale, which meant the same program read the same file differently on two machines --
     * the kind of bug that only appears once the file crosses a border.
     */
    public static Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }

    /** The canonical name of this charset. */
    public final String name() {
        return this.canonicalName;
    }

    /**
     * The other names this charset answers to.
     *
     * <p><strong>Deliberate divergence:</strong> a fresh copy, where the JDK returns an
     * unmodifiable view. The guarantee a caller actually relies on -- that meddling with the
     * result cannot corrupt the charset -- holds either way, and copying is the convention this
     * library already follows for {@code Map.keySet}.
     */
    public final Set<String> aliases() {
        HashSet<String> out = new HashSet<String>();
        int i = 0;
        while (i < this.aliasNames.length) {
            out.add(this.aliasNames[i]);
            i = i + 1;
        }
        return out;
    }

    // The alias array itself, for the registry, which scans it on every miss. `aliases()` copies
    // into a Set on each call; inside the package that copy buys nothing, and this array is never
    // handed out beyond it.
    String[] aliasArray() {
        return this.aliasNames;
    }

    /** A name for this charset fit for showing to a person; the canonical name here. */
    public String displayName() {
        return this.canonicalName;
    }

    /**
     * A name for this charset fit for showing to a person in the given locale.
     *
     * @param locale accepted and ignored -- the names are not translated
     */
    public String displayName(Locale locale) {
        return this.canonicalName;
    }

    /**
     * Whether this charset is registered with IANA.
     *
     * <p>Decided by the name, which is the rule the registry itself imposes: an unregistered
     * charset must be named with an {@code x-} prefix, so anything without one is registered.
     */
    public final boolean isRegistered() {
        return !this.canonicalName.startsWith("X-") && !this.canonicalName.startsWith("x-");
    }

    /**
     * Whether every character this charset can represent is also representable in {@code cs}.
     *
     * <p>Note the direction, which is easy to get backwards: {@code UTF_8.contains(US_ASCII)} is
     * true because UTF-8 covers everything ASCII does, not the other way round.
     *
     * @param cs the charset to test for containment
     */
    public abstract boolean contains(Charset cs);

    /** A new decoder for this charset. */
    public abstract CharsetDecoder newDecoder();

    /**
     * A new encoder for this charset.
     *
     * @throws UnsupportedOperationException if this charset cannot encode -- see {@link
     *         #canEncode}
     */
    public abstract CharsetEncoder newEncoder();

    /**
     * Whether this charset can encode as well as decode.
     *
     * <p>True for every charset in this library. The ones that answer false are the auto-detecting
     * and decode-only charsets, which exist so that legacy input can be read without offering a
     * way to write more of it.
     */
    public boolean canEncode() {
        return true;
    }

    /**
     * Decodes a whole buffer, replacing anything malformed or unmappable.
     *
     * <p>Never throws a coding exception, by construction: both actions are set to REPLACE, so
     * broken input becomes U+FFFD rather than a failure. Use {@link #newDecoder} when you need
     * to know that the input was bad.
     *
     * @param bb the bytes to decode
     * @return the characters
     */
    public final CharBuffer decode(ByteBuffer bb) {
        CharsetDecoder decoder = this.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return decoder.decode(bb);
        } catch (CharacterCodingException impossible) {
            // Unreachable: REPLACE never reports. If it happens, the decoder is broken.
            throw new Error(impossible);
        }
    }

    /**
     * Encodes a whole buffer, replacing anything that cannot be represented.
     *
     * <p>As with {@link #decode(ByteBuffer)}, never throws: unrepresentable characters become
     * the encoder replacement, which is a question mark for the charsets here.
     *
     * @param cb the characters to encode
     * @return the bytes
     */
    public final ByteBuffer encode(CharBuffer cb) {
        CharsetEncoder encoder = this.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return encoder.encode(cb);
        } catch (CharacterCodingException impossible) {
            // Unreachable: REPLACE never reports. If it happens, the encoder is broken.
            throw new Error(impossible);
        }
    }

    /**
     * Encodes a string, replacing anything that cannot be represented.
     *
     * @param str the text to encode
     * @return the bytes
     */
    public final ByteBuffer encode(String str) {
        CharBuffer wrapped = CharBuffer.wrap(str);
        return this.encode(wrapped);
    }

    /**
     * Orders charsets by canonical name, without regard to case.
     *
     * @param that the charset to compare against
     */
    public final int compareTo(Charset that) {
        return this.canonicalName.compareToIgnoreCase(that.name());
    }

    /** A hash consistent with {@link #equals}, derived from the canonical name. */
    public final int hashCode() {
        return this.canonicalName.hashCode();
    }

    /**
     * Equal when the canonical names are equal.
     *
     * <p>Case-sensitively, unlike {@link #compareTo}, and the asymmetry is the JDK behaviour:
     * lookup is lenient about case but two charsets are the same only if they are spelled the
     * same, and since canonical names come from the registry that never actually bites.
     *
     * @param other the object to compare against
     */
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Charset)) {
            return false;
        }
        Charset that = (Charset) other;
        return this.canonicalName.equals(that.name());
    }

    /** The canonical name. */
    public final String toString() {
        return this.canonicalName;
    }
}
