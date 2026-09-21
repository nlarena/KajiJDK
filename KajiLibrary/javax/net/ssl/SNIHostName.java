package javax.net.ssl;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A host name in the SNI extension: the only type that exists today.
 *
 * <h2>Why the name is kept in ASCII and not as it was written</h2>
 *
 * <p>Because the protocol carries bytes and a domain name may have accents. The conversion to the
 * ASCII form (IDN's {@code xn--...}) has to happen <strong>only once and in a single
 * place</strong>: two ends that do it differently do not recognise each other, and the server sends
 * the wrong certificate or cuts off.
 *
 * <p>This class does not implement that conversion — it lives in {@code java.net.IDN} — but it does
 * fix the other half of the rule: the name is compared <strong>case-insensitively</strong>, because
 * domain names do not distinguish case and {@code Example.com} has to match {@code example.com}.
 */
public final class SNIHostName extends SNIServerName {

    private final String hostname;

    /**
     * From a host name.
     *
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalArgumentException if it is empty or ends in a dot — an absolute name with the
     *     final dot is valid in DNS but not in SNI, and accepting it would produce a comparison
     *     that never matches
     */
    public SNIHostName(String hostname) {
        super(StandardConstants.SNI_HOST_NAME, bytes(hostname));
        this.hostname = hostname;
    }

    /**
     * From the bytes as they came from the protocol.
     *
     * @throws IllegalArgumentException if they are not seven-bit ASCII, or if they do not form a
     *     valid name
     */
    public SNIHostName(byte[] encoded) {
        super(StandardConstants.SNI_HOST_NAME, encoded);
        this.hostname = validate(new String(encoded, StandardCharsets.US_ASCII));
    }

    private static byte[] bytes(String hostname) {
        if (hostname == null) {
            throw new NullPointerException("hostname");
        }
        return validate(hostname).getBytes(StandardCharsets.US_ASCII);
    }

    private static String validate(String hostname) {
        if (hostname.isEmpty()) {
            throw new IllegalArgumentException("the host name is empty");
        }
        if (hostname.endsWith(".")) {
            throw new IllegalArgumentException("the host name ends in a dot: " + hostname);
        }
        for (int i = 0; i < hostname.length(); i++) {
            if (hostname.charAt(i) > 127) {
                throw new IllegalArgumentException(
                        "the host name is not ASCII; convert it first with java.net.IDN");
            }
        }
        return hostname;
    }

    /** The name in its ASCII form. */
    public String getAsciiName() {
        return this.hostname;
    }

    /**
     * Case-insensitive — see the class note.
     *
     * <p>The {@link Locale#ENGLISH} in the {@code toLowerCase} is not decoration: with Turkish,
     * {@code "I"} lowers to a dotless i and {@code "INDEX"} would stop matching {@code "index"}. A
     * domain name does not depend on the language of whoever runs the program.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SNIHostName) {
            SNIHostName o = (SNIHostName) other;
            return this.hostname.equalsIgnoreCase(o.hostname);
        }
        return false;
    }

    public int hashCode() {
        return 31 * 17 + this.hostname.toLowerCase(Locale.ENGLISH).hashCode();
    }

    public String toString() {
        return "type=host_name (0), value=" + this.hostname;
    }

    /**
     * An {@link SNIMatcher} that accepts the host names matching that regular expression.
     *
     * <p>Regular and not a literal list because a server serves families of names --a whole domain
     * and its subdomains-- and enumerating them would be impossible.
     *
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws java.util.regex.PatternSyntaxException if the expression does not compile
     */
    public static SNIMatcher createSNIMatcher(String regex) {
        if (regex == null) {
            throw new NullPointerException("regex");
        }
        return new SNIHostNameMatcher(regex);
    }
}
