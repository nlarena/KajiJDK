package javax.management.remote;

import java.io.Serializable;
import java.net.MalformedURLException;

/**
 * KajiLibrary's javax.management.remote.JMXServiceURL -- a JMX connector's address.
 *
 * <p>The form is {@code service:jmx:<protocol>://[<host>][:<port>][<path>]}. The
 * {@code service:jmx:} prefix comes from the service URL standard and is mandatory.
 *
 * <h2>The host grammar, which is stricter than it looks</h2>
 *
 * <p>One of three things is accepted:
 *
 * <ul>
 *   <li>an IPv4 address in four numbers, each from 0 to 255. {@code 1.2.3.4} is valid;
 *       {@code 1.2.3}, {@code 1.2.3.4.5} and {@code 256.1.1.1} are not;
 *   <li>a numeric IPv6 address, with or without brackets. It is detected by having colons, and
 *       that is why any host with a {@code :} inside is validated as IPv6 and fails if it is not;
 *   <li>a machine name: labels separated by dots, each of letters, digits and hyphens, neither
 *       starting nor ending in a hyphen. The <b>first</b> label may start with a digit
 *       --{@code 12a.b} is valid-- but the following ones have to start with a letter, so
 *       {@code abc.123} is not valid. It is the rule that keeps a name from being confused with
 *       an address.
 * </ul>
 *
 * <p>An empty host is allowed and means "the local machine, without saying which"; in that case
 * the port has to be 0. Passing null to the three- and four-argument constructors is different:
 * there the machine's name is resolved.
 *
 * <h2>{@code hashCode} consistent with {@code equals}</h2>
 *
 * <p>{@link #equals} ignores case in the host, as DNS commands. The JDK computes
 * {@link #hashCode} over {@link #toString}, which keeps the host's capitals, so two equal URLs
 * may have different hashes -- it was checked against JDK 25 and that is how it is. That breaks
 * {@code Object}'s contract and makes a hash table with these keys fail.
 *
 * <p>Here the hash is computed over the form {@code equals} uses, with the host in lower case. It
 * is this class's only deliberate divergence and it is in favour of the contract.
 */
public class JMXServiceURL implements Serializable {

    private static final long serialVersionUID = 8173364409860779292L;

    /** The protocol, in lower case. */
    private final String protocol;

    /** The host, with whatever capitals were passed. */
    private final String host;

    /** The port; 0 means no port. */
    private final int port;

    /** The path, or empty. */
    private final String urlPath;

    /** The text form, which does not change. */
    private transient String toString;

    /**
     * Parses a complete URL.
     *
     * @throws MalformedURLException if it does not start with {@code service:jmx:} or something
     *     does not add up
     * @throws NullPointerException if it is null
     */
    public JMXServiceURL(String serviceURL) throws MalformedURLException {
        final String prefix = "service:jmx:";
        if (!serviceURL.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new MalformedURLException("Service URL must start with " + prefix);
        }
        int at = prefix.length();
        final int sep = serviceURL.indexOf("://", at);
        if (sep < 0) {
            throw new MalformedURLException("Missing \"://\" after protocol name");
        }
        String proto = serviceURL.substring(at, sep);
        at = sep + 3;
        String rest = serviceURL.substring(at);
        String hostPart;
        String pathPart;
        int cut = firstIndexOf(rest, "/;");
        if (cut < 0) {
            hostPart = rest;
            pathPart = "";
        } else {
            hostPart = rest.substring(0, cut);
            pathPart = rest.substring(cut);
        }
        String hostText;
        int portValue = 0;
        if (hostPart.startsWith("[")) {
            int close = hostPart.indexOf(']');
            if (close < 0) {
                throw new MalformedURLException("Bad IPv6 address: " + hostPart);
            }
            hostText = hostPart.substring(0, close + 1);
            String after = hostPart.substring(close + 1);
            portValue = parsePort(after);
        } else {
            int colon = hostPart.lastIndexOf(':');
            if (colon >= 0) {
                hostText = hostPart.substring(0, colon);
                portValue = parsePort(hostPart.substring(colon));
            } else {
                hostText = hostPart;
            }
        }
        this.protocol = proto.toLowerCase();
        this.host = unbracket(hostText);
        this.port = portValue;
        this.urlPath = pathPart;
        validate();
    }

    /**
     * Builds a URL without a path.
     *
     * @param protocol the protocol; null means {@code jmxmp}
     * @param host the host; null means this machine's name
     * @param port the port, or 0
     * @throws MalformedURLException if something does not add up
     */
    public JMXServiceURL(String protocol, String host, int port) throws MalformedURLException {
        this(protocol, host, port, null);
    }

    /**
     * Builds a complete URL.
     *
     * @param urlPath the path; it has to start with {@code /} or {@code ;}, or be empty or null
     * @throws MalformedURLException if something does not add up
     */
    public JMXServiceURL(String protocol, String host, int port, String urlPath)
        throws MalformedURLException {
        if (protocol == null) {
            protocol = "jmxmp";
        }
        if (host == null) {
            host = localHostName();
        }
        if (host.startsWith("[")) {
            if (!host.endsWith("]")) {
                throw new MalformedURLException("Host starts with [ but does not end with ]");
            }
            host = host.substring(1, host.length() - 1);
            if (!isNumericIPv6Address(host)) {
                throw new MalformedURLException(
                    "Address inside [...] must be numeric IPv6 address");
            }
        }
        if (urlPath == null) {
            urlPath = "";
        }
        this.protocol = protocol.toLowerCase();
        this.host = host;
        this.port = port;
        this.urlPath = urlPath;
        validate();
    }

    /** The protocol, always in lower case. */
    public String getProtocol() {
        return this.protocol;
    }

    /** The host, without brackets even if it is IPv6, and with the original capitals. */
    public String getHost() {
        return this.host;
    }

    /** The port, or 0 if none was given. */
    public int getPort() {
        return this.port;
    }

    /** The path, or empty. */
    public String getURLPath() {
        return this.urlPath;
    }

    /**
     * The complete URL.
     *
     * <p>Port 0 is not written, and an IPv6 host comes out between brackets.
     */
    @Override
    public String toString() {
        if (this.toString != null) {
            return this.toString;
        }
        StringBuilder sb = new StringBuilder("service:jmx:");
        sb.append(this.protocol).append("://");
        if (isNumericIPv6Address(this.host)) {
            sb.append('[').append(this.host).append(']');
        } else {
            sb.append(this.host);
        }
        if (this.port != 0) {
            sb.append(':').append(this.port);
        }
        sb.append(this.urlPath);
        this.toString = sb.toString();
        return this.toString;
    }

    /**
     * Protocol and host without distinguishing case, the same port, the exact path.
     *
     * <p>That the path does distinguish is not an oversight: it may be a JNDI name, and those do
     * distinguish.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JMXServiceURL)) {
            return false;
        }
        JMXServiceURL other = (JMXServiceURL) obj;
        return this.protocol.equalsIgnoreCase(other.protocol)
            && this.host.equalsIgnoreCase(other.host)
            && this.port == other.port
            && this.urlPath.equals(other.urlPath);
    }

    /** Consistent with {@link #equals}. See the class note. */
    @Override
    public int hashCode() {
        return this.protocol.hashCode() * 31 * 31 * 31
            + this.host.toLowerCase().hashCode() * 31 * 31
            + this.port * 31
            + this.urlPath.hashCode();
    }

    /** This machine's name, or {@code localhost} if it cannot be found out. */
    private static String localHostName() throws MalformedURLException {
        String name;
        try {
            name = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Throwable e) {
            return "localhost";
        }
        if (name == null || !isValidHostName(name)) {
            return "localhost";
        }
        return name;
    }

    /** Removes the brackets from an IPv6 host written with them. */
    private static String unbracket(String h) {
        if (h.length() >= 2 && h.charAt(0) == '[' && h.charAt(h.length() - 1) == ']') {
            return h.substring(1, h.length() - 1);
        }
        return h;
    }

    /** The first index where one of those characters appears, or -1. */
    private static int firstIndexOf(String s, String chars) {
        int i = 0;
        while (i < s.length()) {
            if (chars.indexOf(s.charAt(i)) >= 0) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Reads {@code ":1234"}; empty means 0. */
    private static int parsePort(String s) throws MalformedURLException {
        if (s.length() == 0) {
            return 0;
        }
        if (s.charAt(0) != ':') {
            throw new MalformedURLException("Bad port number: \"" + s + "\"");
        }
        String digits = s.substring(1);
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new MalformedURLException("Bad port number: \"" + digits + "\": " + e);
        }
    }

    /** The three checks the constructors share. */
    private void validate() throws MalformedURLException {
        if (!isValidProtocol(this.protocol)) {
            throw new MalformedURLException(
                "Missing or invalid protocol name: \"" + this.protocol + "\"");
        }
        if (this.host.length() == 0) {
            if (this.port != 0) {
                throw new MalformedURLException("Cannot give port number without host name");
            }
        } else if (isNumericIPv6Address(this.host)) {
            if (!isWellFormedIPv6(this.host)) {
                throw new MalformedURLException("Bad IPv6 address: " + this.host);
            }
        } else if (!isValidHostName(this.host)) {
            throw new MalformedURLException("Bad host: \"" + this.host + "\"");
        }
        if (this.port < 0) {
            throw new MalformedURLException("Bad port: " + this.port);
        }
        if (this.urlPath.length() > 0 && !this.urlPath.startsWith("/")
            && !this.urlPath.startsWith(";")) {
            throw new MalformedURLException("Bad URL path: " + this.urlPath);
        }
    }

    /** A letter followed by letters, digits, {@code +} and {@code -}. */
    private static boolean isValidProtocol(String p) {
        if (p.length() == 0 || !isAlpha(p.charAt(0))) {
            return false;
        }
        int i = 1;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (!isAlpha(c) && !isDigit(c) && c != '+' && c != '-') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** An IPv4 in four numbers, or a name. See the class note. */
    private static boolean isValidHostName(String h) {
        if (isNumericIPv4(h)) {
            return true;
        }
        int start = 0;
        boolean first = true;
        while (true) {
            int dot = h.indexOf('.', start);
            int end;
            if (dot < 0) {
                end = h.length();
            } else {
                end = dot;
            }
            if (!isValidLabel(h, start, end, first)) {
                return false;
            }
            if (dot < 0) {
                return true;
            }
            start = dot + 1;
            first = false;
        }
    }

    /**
     * A name label.
     *
     * @param first whether it is the first one; only that one may start with a digit
     */
    private static boolean isValidLabel(String h, int start, int end, boolean first) {
        if (end <= start) {
            return false;
        }
        char firstChar = h.charAt(start);
        if (first) {
            if (!isAlpha(firstChar) && !isDigit(firstChar)) {
                return false;
            }
        } else if (!isAlpha(firstChar)) {
            return false;
        }
        if (!isAlpha(h.charAt(end - 1)) && !isDigit(h.charAt(end - 1))) {
            return false;
        }
        int i = start;
        while (i < end) {
            char c = h.charAt(i);
            if (!isAlpha(c) && !isDigit(c) && c != '-') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Four numbers from 0 to 255 separated by dots. */
    private static boolean isNumericIPv4(String s) {
        int start = 0;
        int parts = 0;
        while (parts < 4) {
            int dot = s.indexOf('.', start);
            int end;
            if (parts == 3) {
                if (dot >= 0) {
                    return false;
                }
                end = s.length();
            } else {
                if (dot < 0) {
                    return false;
                }
                end = dot;
            }
            if (end <= start || end - start > 3) {
                return false;
            }
            int value = 0;
            int i = start;
            while (i < end) {
                char c = s.charAt(i);
                if (!isDigit(c)) {
                    return false;
                }
                value = value * 10 + (c - '0');
                i = i + 1;
            }
            if (value > 255) {
                return false;
            }
            start = end + 1;
            parts = parts + 1;
        }
        return true;
    }

    /** A host with colons is treated as IPv6, valid or not. It is what the JDK does. */
    private static boolean isNumericIPv6Address(String s) {
        return s.indexOf(':') >= 0;
    }

    /**
     * Whether that text is a well-formed IPv6 literal.
     *
     * <p>It accepts the {@code ::} abbreviation once, an IPv4 in the last group, and a numeric
     * scope identifier after {@code %}.
     *
     * <p>A <b>named</b> scope --{@code %eth0}-- is rejected. The JDK accepts it only if that
     * interface exists on the machine, so it is not a property of the text but of the equipment;
     * with no interfaces to consult, rejecting it is the only thing that can be said with
     * certainty.
     */
    private static boolean isWellFormedIPv6(String s) {
        int pct = s.indexOf('%');
        if (pct >= 0) {
            String scope = s.substring(pct + 1);
            if (scope.length() == 0) {
                return false;
            }
            int i = 0;
            while (i < scope.length()) {
                if (!isDigit(scope.charAt(i))) {
                    return false;
                }
                i = i + 1;
            }
            s = s.substring(0, pct);
        }
        int dbl = s.indexOf("::");
        String head;
        String tail;
        if (dbl >= 0) {
            if (s.indexOf("::", dbl + 1) >= 0) {
                return false;
            }
            head = s.substring(0, dbl);
            tail = s.substring(dbl + 2);
        } else {
            head = s;
            tail = null;
        }
        int[] headCount = new int[1];
        if (!countGroups(head, headCount, tail == null)) {
            return false;
        }
        if (tail == null) {
            return headCount[0] == 8;
        }
        int[] tailCount = new int[1];
        if (!countGroups(tail, tailCount, true)) {
            return false;
        }
        return headCount[0] + tailCount[0] <= 7;
    }

    /**
     * Counts a half's groups and says whether they are well formed.
     *
     * @param allowIPv4 whether the last group may be an IPv4, which counts as two
     */
    private static boolean countGroups(String s, int[] count, boolean allowIPv4) {
        if (s.length() == 0) {
            count[0] = 0;
            return true;
        }
        int total = 0;
        int start = 0;
        while (true) {
            int colon = s.indexOf(':', start);
            int end;
            if (colon < 0) {
                end = s.length();
            } else {
                end = colon;
            }
            if (colon < 0 && allowIPv4 && s.indexOf('.', start) >= 0) {
                if (!isNumericIPv4(s.substring(start))) {
                    return false;
                }
                total = total + 2;
                count[0] = total;
                return true;
            }
            if (end <= start || end - start > 4) {
                return false;
            }
            int i = start;
            while (i < end) {
                if (!isHex(s.charAt(i))) {
                    return false;
                }
                i = i + 1;
            }
            total = total + 1;
            if (colon < 0) {
                count[0] = total;
                return true;
            }
            start = colon + 1;
        }
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHex(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
