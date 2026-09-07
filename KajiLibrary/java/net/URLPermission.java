package java.net;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Permission to talk to a URL: which scheme, which host, which path, with which methods and sending
// which headers.
//
// It is the permission in this package that can be written whole with no network, and the reason is
// in its own contract: **URLPermission does not resolve names**. It compares the texts as they were
// given -- it does not canonicalize the host, does not consult DNS, does not do a reverse lookup.
// That makes it pure computation over strings, which is exactly what can be done here.
//
// The path grammar has three forms and the difference matters:
//
//   /a/b     exactly that one
//   /a/*     that one and **one** level more: it covers /a/b but not /a/b/c
//   /a/-     that one and everything hanging off it, recursively
//
// The actions are "methods:headers", both comma-separated lists, and `*` in either of the two means
// all. They are normalized --sorted, and always with the colon-- so that two permissions saying the
// same thing compare equal: "POST,GET" and "GET,POST" are the same permission.
//
// ===========================================================================================
// HOW THIS DIFFERS FROM `SocketPermission`
// ===========================================================================================
//
// `SocketPermission.implies` is specified in terms of name resolution: to decide whether the
// permission over "example.org" covers the permission over "1.2.3.4", the first has to be resolved
// and the addresses compared. There is no resolver in this VM, so that class compares textually and
// documents that its answer is stricter than the JDK's, never laxer. This one has no such gap,
// because its contract never asked for a resolver in the first place.
//
// Nothing is omitted from this class.
//
// @deprecated The Security Manager is deprecated for removal; these permissions are no longer
// checked.
@Deprecated
public final class URLPermission extends Permission {

    private static final long serialVersionUID = -2702463814894478682L;

    private String scheme;
    private String ssp;
    private String path;
    private Authority authority;
    private List<String> methods;
    private List<String> requestHeaders;
    private String actions;

    /**
     * The permission over that URL, with those actions.
     *
     * @param url scheme://authority/path, or "scheme:*"
     * @param actions "methods:headers"; the colon is optional if there are no headers
     * @throws IllegalArgumentException if the URL or the actions are not understood
     */
    public URLPermission(String url, String actions) {
        super(url);
        this.init(actions);
    }

    /** The permission over that URL for every method and every header ("*:*"). */
    public URLPermission(String url) {
        this(url, "*:*");
    }

    private void init(String actions) {
        this.parseURI(this.getName());
        int colon = actions.indexOf(':');
        // A second ':' would mean a third field that does not exist; it is a writing error, not a
        // header list with a colon inside it.
        if (actions.lastIndexOf(':') != colon) {
            throw new IllegalArgumentException("Invalid actions string: \"" + actions + "\"");
        }
        String meths;
        String heads;
        if (colon == -1) {
            meths = actions;
            heads = "";
        } else {
            meths = actions.substring(0, colon);
            heads = actions.substring(colon + 1);
        }
        this.methods = normalize(meths, "methods");
        this.requestHeaders = normalize(heads, "headers");
        this.actions = this.buildActions();
    }

    private void parseURI(String url) {
        int len = url.length();
        int delim = url.indexOf(':');
        if (delim == -1 || delim + 1 == len) {
            throw new IllegalArgumentException("Invalid URL string: \"" + url + "\"");
        }
        this.scheme = url.substring(0, delim).toLowerCase();
        this.ssp = url.substring(delim + 1);
        if (!this.ssp.startsWith("//")) {
            // The only authority-less form admitted is "scheme:*": that whole scheme.
            if (!this.ssp.equals("*")) {
                throw new IllegalArgumentException("Invalid URL string: \"" + url + "\"");
            }
            this.authority = new Authority(this.scheme, "*");
            return;
        }
        String authpath = this.ssp.substring(2);
        delim = authpath.indexOf('/');
        String auth;
        if (delim == -1) {
            this.path = "";
            auth = authpath;
        } else {
            auth = authpath.substring(0, delim);
            this.path = authpath.substring(delim);
        }
        this.authority = new Authority(this.scheme, auth.toLowerCase());
    }

    // Sorted and in canonical case, so that two equivalent permissions have the same actions string.
    // `field` is "methods" or "headers" and it decides two things: how the case is normalized and what
    // the error message says.
    //
    // Duplicates are NOT removed --"GET,GET" stays "GET,GET"-- because it is what the JDK does, and
    // the actions string is observable through `getActions`. It had been done the other way round and
    // the behaviour test caught it.
    //
    // White space is an error, not something to trim: a permission written " GET " almost always
    // comes from a hand-assembled string gone wrong, and accepting it silently gives a permission
    // that is not the one that was meant. The JDK throws, and the message quotes the WHOLE field
    // untouched, not the token.
    private static List<String> normalize(String s, String field) {
        List<String> out = new ArrayList<String>();
        if (s == null || s.length() == 0) {
            return Collections.unmodifiableList(out);
        }
        int i = 0;
        while (i < s.length()) {
            if (Character.isWhitespace(s.charAt(i))) {
                throw new IllegalArgumentException(
                        "White space not allowed in " + field + ": \"" + s + "\"");
            }
            i = i + 1;
        }
        int start = 0;
        while (start <= s.length()) {
            int comma = s.indexOf(',', start);
            String tok;
            if (comma == -1) {
                tok = s.substring(start);
                start = s.length() + 1;
            } else {
                tok = s.substring(start, comma);
                start = comma + 1;
            }
            if (tok.length() > 0) {
                out.add("methods".equals(field) ? tok.toUpperCase() : canonHeader(tok));
            }
        }
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    // "accept" -> "Accept", "x-Y-z" -> "X-Y-Z": an initial capital on each hyphen-separated stretch,
    // lower case for the rest. It is an HTTP header's canonical spelling, and normalizing to it
    // --instead of to lower case-- is what makes `getActions` return something that can be pasted
    // straight into a header.
    private static String canonHeader(String tok) {
        StringBuilder b = new StringBuilder();
        boolean atStart = true;
        int i = 0;
        while (i < tok.length()) {
            char c = tok.charAt(i);
            if (c == '-') {
                b.append(c);
                atStart = true;
            } else {
                b.append(atStart ? Character.toUpperCase(c) : Character.toLowerCase(c));
                atStart = false;
            }
            i = i + 1;
        }
        return b.toString();
    }

    private String buildActions() {
        StringBuilder b = new StringBuilder();
        String sep = "";
        int i = 0;
        while (i < this.methods.size()) {
            b.append(sep).append(this.methods.get(i));
            sep = ",";
            i = i + 1;
        }
        b.append(':');
        sep = "";
        i = 0;
        while (i < this.requestHeaders.size()) {
            b.append(sep).append(this.requestHeaders.get(i));
            sep = ",";
            i = i + 1;
        }
        return b.toString();
    }

    /** The normalized actions, in the form "methods:headers". */
    public String getActions() {
        return this.actions;
    }

    /** Whether this permission covers {@code p}. See the header for the three path forms. */
    public boolean implies(Permission p) {
        if (!(p instanceof URLPermission)) {
            return false;
        }
        URLPermission that = (URLPermission) p;
        if (this.methods.isEmpty() && !that.methods.isEmpty()) {
            return false;
        }
        if (!this.methods.isEmpty() && !this.methods.get(0).equals("*")
                && !this.methods.containsAll(that.methods)) {
            return false;
        }
        if (this.requestHeaders.isEmpty() && !that.requestHeaders.isEmpty()) {
            return false;
        }
        if (!this.requestHeaders.isEmpty() && !this.requestHeaders.get(0).equals("*")
                && !this.requestHeaders.containsAll(that.requestHeaders)) {
            return false;
        }
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (this.ssp.equals("*")) {
            return true;
        }
        if (!this.authority.implies(that.authority)) {
            return false;
        }
        if (this.path == null) {
            return that.path == null;
        }
        if (that.path == null) {
            return false;
        }
        if (this.path.endsWith("/-")) {
            String prefix = this.path.substring(0, this.path.length() - 1);
            return that.path.startsWith(prefix);
        }
        if (this.path.endsWith("/*")) {
            String prefix = this.path.substring(0, this.path.length() - 1);
            if (!that.path.startsWith(prefix)) {
                return false;
            }
            String suffix = that.path.substring(prefix.length());
            // A single level: if a slash is left, the other path goes deeper than allowed. And a "-"
            // as a suffix would be a recursive wildcard slipping in through the back door.
            return suffix.indexOf('/') == -1 && !suffix.equals("-");
        }
        return this.path.equals(that.path);
    }

    public boolean equals(Object p) {
        if (!(p instanceof URLPermission)) {
            return false;
        }
        URLPermission that = (URLPermission) p;
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (!this.getActions().equals(that.getActions())) {
            return false;
        }
        if (!this.authority.equals(that.authority)) {
            return false;
        }
        if (this.path != null) {
            return this.path.equals(that.path);
        }
        return that.path == null;
    }

    public int hashCode() {
        return this.getActions().hashCode()
                + this.scheme.hashCode()
                + this.authority.hashCode()
                + (this.path == null ? 0 : this.path.hashCode());
    }

    // The URL's authority: host --with its wildcards-- and port range.
    //
    // A host may be "*" (any), "*.domain" (any inside that domain) or an exact name/literal. **It is
    // never resolved**: see the class's header.
    private static class Authority {

        private final String host;
        private final int portLow;
        private final int portHigh;

        Authority(String scheme, String authority) {
            String h = authority;
            int low;
            int high;
            int colon = h.lastIndexOf(':');
            // A ':' inside brackets belongs to an IPv6 literal, not to the port separator.
            int bracket = h.lastIndexOf(']');
            if (colon != -1 && colon > bracket) {
                String p = h.substring(colon + 1);
                h = h.substring(0, colon);
                if (p.equals("*")) {
                    low = 0;
                    high = 65535;
                } else {
                    int dash = p.indexOf('-');
                    if (dash == -1) {
                        low = parsePort(p);
                        high = low;
                    } else if (dash == 0) {
                        low = 0;
                        high = parsePort(p.substring(1));
                    } else if (dash == p.length() - 1) {
                        low = parsePort(p.substring(0, dash));
                        high = 65535;
                    } else {
                        low = parsePort(p.substring(0, dash));
                        high = parsePort(p.substring(dash + 1));
                    }
                }
            } else {
                // With no port the scheme's applies: comparing "http://x.com" with "http://x.com:80"
                // has to give the same, because they name the same thing.
                low = defaultPort(scheme);
                high = low;
            }
            this.host = h;
            this.portLow = low;
            this.portHigh = high;
        }

        private static int parsePort(String p) {
            try {
                int v = Integer.parseInt(p);
                if (v < 0 || v > 65535) {
                    throw new IllegalArgumentException("Invalid port range");
                }
                return v;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port range");
            }
        }

        private static int defaultPort(String scheme) {
            if (scheme.equals("http")) {
                return 80;
            }
            if (scheme.equals("https")) {
                return 443;
            }
            return -1;
        }

        boolean implies(Authority that) {
            if (that.portLow < this.portLow || that.portHigh > this.portHigh) {
                return false;
            }
            if (this.host.equals("*")) {
                return true;
            }
            if (this.host.startsWith("*.")) {
                String domain = this.host.substring(1);
                return that.host.endsWith(domain);
            }
            return this.host.equals(that.host);
        }

        public boolean equals(Object o) {
            if (!(o instanceof Authority)) {
                return false;
            }
            Authority a = (Authority) o;
            return this.host.equals(a.host) && this.portLow == a.portLow
                    && this.portHigh == a.portHigh;
        }

        public int hashCode() {
            return this.host.hashCode() + this.portLow + this.portHigh;
        }
    }
}
