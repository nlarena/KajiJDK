package java.net;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

// A URI per RFC 3986, broken into its five pieces: scheme, authority, path, query and fragment. It
// is a VALUE type: immutable, with equality by content.
//
// It came into the library because `javax.tools.FileObject.toUri()` and `SimpleJavaFileObject`'s
// constructor ask for it -- without it, that package cannot be closed.
//
// COMPLETE: all 32 members. This note used to say that the %XX decoding was missing --that the
// `getX()` returned the same as the `getRawX()` because `java.nio.charset` did not exist-- and both
// halves of that sentence went stale: the package is here (it is imported above) and so is the
// decoding. `getPath()` over `/a%20b/c%2Fd` returns `/a b/c/d` and `getRawPath()` the escaped form,
// which is what the JDK does; it was checked against it.
//
// The parser uses `charAt` and `substring(int,int)` because our `String` has neither `indexOf` nor
// `substring(int)` -- hence the `scan*` methods below.
public final class URI implements Comparable<URI>, Serializable {

    private final String string;
    private final String scheme;
    private final String ssp;          // scheme-specific part: everything after the "scheme:"
    private final String authority;
    private final String path;
    private final String query;
    private final String fragment;
    private final boolean opaque;

    public URI(String str) throws URISyntaxException {
        if (str == null) {
            throw new NullPointerException();
        }
        this.string = str;
        int len = str.length();

        // 1. scheme: letters/digits/+-. up to the first ':', and only if it appears before any '/',
        //    '?' or '#'. "foo/bar:baz" has NO scheme.
        int colon = -1;
        int i = 0;
        while (i < len) {
            char c = str.charAt(i);
            if (c == ':') { colon = i; break; }
            if (c == '/' || c == '?' || c == '#') { break; }
            i = i + 1;
        }
        String sch = null;
        int rest = 0;
        if (colon > 0) {
            if (!isSchemeStart(str.charAt(0))) {
                throw new URISyntaxException(str, "Illegal character in scheme name", 0);
            }
            int k = 1;
            while (k < colon) {
                if (!isSchemeChar(str.charAt(k))) {
                    throw new URISyntaxException(str, "Illegal character in scheme name", k);
                }
                k = k + 1;
            }
            sch = str.substring(0, colon);
            rest = colon + 1;
        } else if (colon == 0) {
            throw new URISyntaxException(str, "Expected scheme name", 0);
        }
        this.scheme = sch;

        // 2. fragment: from the last '#' to the end (there cannot be another after it).
        int hash = scanFor(str, rest, len, '#');
        int endOfRest = (hash < 0) ? len : hash;
        this.fragment = (hash < 0) ? null : str.substring(hash + 1, len);

        this.ssp = str.substring(rest, endOfRest);

        // 3. Opaque = it has a scheme and its scheme-specific part does NOT start with '/'
        //    ("mailto:x@y"). In that case there is no authority, path or query to separate.
        this.opaque = (sch != null) && !(this.ssp.length() > 0 && this.ssp.charAt(0) == '/');
        if (this.opaque) {
            if (this.ssp.isEmpty()) {
                throw new URISyntaxException(str, "Expected scheme-specific part", rest);
            }
            this.authority = null;
            this.path = null;
            this.query = null;
            return;
        }

        // 4. hierarchical: [ "//" authority ] path [ "?" query ]
        int p = rest;
        String auth = null;
        if (p + 1 < endOfRest && str.charAt(p) == '/' && str.charAt(p + 1) == '/') {
            int authStart = p + 2;
            int authEnd = authStart;
            while (authEnd < endOfRest) {
                char c = str.charAt(authEnd);
                if (c == '/' || c == '?') { break; }
                authEnd = authEnd + 1;
            }
            auth = str.substring(authStart, authEnd);
            p = authEnd;
        }
        this.authority = auth;

        int qmark = scanFor(str, p, endOfRest, '?');
        int pathEnd = (qmark < 0) ? endOfRest : qmark;
        this.path = str.substring(p, pathEnd);
        this.query = (qmark < 0) ? null : str.substring(qmark + 1, endOfRest);
    }

    // The same as the constructor but for strings known to be well formed (the program's
    // constants): it turns the failure into an unchecked one, as in the JDK.
    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage());
        }
    }

    public String getScheme() { return this.scheme; }

    // Absolute = it has a scheme. It is what tells "http://a/b" from "/b".
    public boolean isAbsolute() { return this.scheme != null; }

    // Opaque = "mailto:x@y": there is a scheme but what follows is not a hierarchical path.
    public boolean isOpaque() { return this.opaque; }

    public String getSchemeSpecificPart() { return unescape(this.ssp); }

    public String getRawSchemeSpecificPart() { return this.ssp; }

    public String getAuthority() { return unescape(this.authority); }

    public String getRawAuthority() { return this.authority; }

    // Of "user@host:port", the part before the '@', already decoded.
    public String getUserInfo() { return unescape(getRawUserInfo()); }

    public String getRawUserInfo() {
        if (this.authority == null) { return null; }
        int at = scanFor(this.authority, 0, this.authority.length(), '@');
        return (at < 0) ? null : this.authority.substring(0, at);
    }

    public String getHost() {
        if (this.authority == null) { return null; }
        int len = this.authority.length();
        int start = scanFor(this.authority, 0, len, '@') + 1;   // -1 + 1 = 0 if there is no '@'
        int colon = scanFor(this.authority, start, len, ':');
        int end = (colon < 0) ? len : colon;
        return this.authority.substring(start, end);
    }

    // -1 when there is no port, just as in the JDK.
    public int getPort() {
        if (this.authority == null) { return -1; }
        int len = this.authority.length();
        int start = scanFor(this.authority, 0, len, '@') + 1;
        int colon = scanFor(this.authority, start, len, ':');
        if (colon < 0 || colon + 1 >= len) { return -1; }
        int value = 0;
        int i = colon + 1;
        while (i < len) {
            char c = this.authority.charAt(i);
            if (c < '0' || c > '9') { return -1; }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return value;
    }

    // ---- the four by-parts constructors -----------------------------------------------------------
    //
    // They take the components RAW and escape them; the single-`String` constructor takes them
    // already escaped. That is the whole difference between the two, and it is what gives the
    // `getPath()`/`getRawPath()` pair its point: without escaping here, "raw" had nothing different
    // to show.
    //
    // The escaping goes in the constructors and **not** inside `build`, even though it would be in
    // a single place there: `build` is also used by `resolve`, `normalize` and `relativize`, which
    // pass it components that are ALREADY escaped --they come out of `this.path`, `this.query`.
    // Escaping there would escape them twice, and a `%20` would become `%2520` on every `resolve`.
    //
    // They assemble the string and then **reparse** it. It may look like a detour --the pieces are
    // in hand-- but it is what guarantees that a URI built from parts and one parsed from the same
    // text are indistinguishable: same `toString`, same `equals`, same `hashCode`. Building the
    // fields by hand would open the door to a URI whose text does not match its parts.

    /** An opaque URI: scheme, scheme-specific part and fragment. */
    public URI(String scheme, String ssp, String fragment) throws URISyntaxException {
        this(build(scheme, null, null, -1, escape(ssp, LEGAL_URIC), null,
                escape(fragment, LEGAL_URIC), true));
    }

    /** A hierarchical URI with a raw authority. */
    public URI(String scheme, String authority, String path, String query, String fragment)
            throws URISyntaxException {
        this(build(scheme, escape(authority, LEGAL_AUTHORITY), null, -1,
                escape(path, LEGAL_PATH), escape(query, LEGAL_URIC),
                escape(fragment, LEGAL_URIC), false));
    }

    /** A hierarchical URI with the authority split into user, host and port. */
    public URI(String scheme, String userInfo, String host, int port, String path, String query,
            String fragment) throws URISyntaxException {
        this(build(scheme, null,
                buildAuthority(escape(userInfo, LEGAL_USERINFO), host, port), port,
                escape(path, LEGAL_PATH), escape(query, LEGAL_URIC),
                escape(fragment, LEGAL_URIC), false));
    }

    /** The one above without user or port, which is the common case of an `http://host/path`. */
    public URI(String scheme, String host, String path, String fragment) throws URISyntaxException {
        this(scheme, null, host, -1, path, null, fragment);
    }

    private static String buildAuthority(String userInfo, String host, int port) {
        if (host == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (userInfo != null) {
            sb.append(userInfo);
            sb.append('@');
        }
        sb.append(host);
        if (port >= 0) {
            sb.append(':');
            sb.append(port);
        }
        return sb.toString();
    }

    private static String build(String scheme, String authority, String userInfo, int port,
            String rest, String query, String fragment, boolean opaque) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (!opaque) {
            String auth = authority != null ? authority : userInfo;
            if (auth != null) {
                sb.append("//");
                sb.append(auth);
            }
        }
        if (rest != null) {
            sb.append(rest);
        }
        if (query != null) {
            sb.append('?');
            sb.append(query);
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        return sb.toString();
    }

    // ---- the algebra of paths --------------------------------------------------------------------

    /**
     * This URI with the `.` and `..` of its path resolved.
     *
     * <p>The trap is in the `..`, and it is the reason this is not a `replace`: removing a `..` is
     * **not** deleting the previous segment if that segment was itself a `..`. `a/../../b` is
     * `../b`, not `b` -- a relative URI may legitimately climb higher than its own text.
     *
     * <p>An opaque URI has no path to normalize and is returned as it stands.
     */
    public URI normalize() {
        if (this.opaque || this.path == null || this.path.length() == 0) {
            return this;
        }
        String cleaned = normalizePath(this.path);
        if (cleaned.equals(this.path)) {
            return this;
        }
        return createOrSame(build(this.scheme, this.authority, null, -1, cleaned, this.query,
                this.fragment, false));
    }

    private static String normalizePath(String path) {
        boolean absolute = path.length() > 0 && path.charAt(0) == '/';
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int from = 0;
        int n = path.length();
        while (from <= n) {
            int cut = scanFor(path, from, n, '/');
            int end = cut < 0 ? n : cut;
            String piece = path.substring(from, end);
            if (piece.equals(".") || piece.length() == 0) {
                // It is discarded -- unless it is the last, where it marks "ends in a slash".
                if (end == n && out.size() > 0) {
                    out.add("");
                }
            } else if (piece.equals("..")) {
                int last = out.size() - 1;
                if (last >= 0 && !out.get(last).equals("..")) {
                    out.remove(last);
                } else if (!absolute) {
                    // In a relative path a `..` with nothing to eat **stays**.
                    out.add("..");
                }
            } else {
                out.add(piece);
            }
            if (cut < 0) {
                from = n + 1;
            } else {
                from = cut + 1;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (absolute) {
            sb.append('/');
        }
        int i = 0;
        while (i < out.size()) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(out.get(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Resolves `that` against this URI, which acts as the base (RFC 3986 §5.2.2).
     *
     * <p>The rules in order, which is how the RFC writes them: if `that` is absolute or opaque, it
     * wins whole; if it has only a fragment, it is pasted onto the base; if its path is absolute,
     * it replaces; and if it is relative, it is hung off the base's directory.
     */
    public URI resolve(URI that) {
        if (that == null) {
            throw new NullPointerException("that");
        }
        if (that.isAbsolute() || that.isOpaque()) {
            return that;
        }
        if (this.isOpaque()) {
            return that;
        }
        // Fragment only: the whole base with the other's fragment.
        if (that.authority == null && (that.path == null || that.path.length() == 0)
                && that.query == null && that.fragment != null) {
            return createOrSame(build(this.scheme, this.authority, null, -1, this.path, this.query,
                    that.fragment, false));
        }
        if (that.authority != null) {
            return createOrSame(build(this.scheme, that.authority, null, -1,
                    normalizePath(nullToEmpty(that.path)), that.query, that.fragment, false));
        }
        String newPath;
        if (that.path != null && that.path.length() > 0 && that.path.charAt(0) == '/') {
            newPath = that.path;
        } else {
            newPath = joinPaths(nullToEmpty(this.path), nullToEmpty(that.path));
        }
        return createOrSame(build(this.scheme, this.authority, null, -1,
                normalizePath(newPath), that.query, that.fragment, false));
    }

    /** The one above, parsing `str` first. */
    public URI resolve(String str) {
        return this.resolve(URI.create(str));
    }

    // The base's path **up to the last slash**: a URI names a resource, not a directory, so what is
    // to the right of the last slash gets replaced.
    private static String joinPaths(String base, String relative) {
        int lastSlash = -1;
        int i = 0;
        while (i < base.length()) {
            if (base.charAt(i) == '/') {
                lastSlash = i;
            }
            i = i + 1;
        }
        if (lastSlash < 0) {
            return relative;
        }
        return base.substring(0, lastSlash + 1) + relative;
    }

    /**
     * Expresses `that` as relative to this URI, if it can.
     *
     * <p>It is {@link #resolve}'s partial inverse: if they do not share scheme and authority, or if
     * `that` does not hang off the base's path, there **is no** relative form and `that` is
     * returned untouched. That is not a failure: it is that the correct answer is the absolute URI.
     */
    public URI relativize(URI that) {
        if (that == null) {
            throw new NullPointerException("that");
        }
        if (this.isOpaque() || that.isOpaque()) {
            return that;
        }
        if (!same(this.scheme, that.scheme) || !same(this.authority, that.authority)) {
            return that;
        }
        String base = normalizePath(nullToEmpty(this.path));
        String other = normalizePath(nullToEmpty(that.path));
        // The prefix is cut at the base's last slash: comparing character by character would leave
        // `/a/bc` as a "child" of `/a/b`, which is false.
        int upToSlash = 0;
        int i = 0;
        while (i < base.length()) {
            if (base.charAt(i) == '/') {
                upToSlash = i + 1;
            }
            i = i + 1;
        }
        String prefix = base.substring(0, upToSlash);
        if (other.length() < prefix.length()) {
            return that;
        }
        if (!other.substring(0, prefix.length()).equals(prefix)) {
            return that;
        }
        String rest = other.substring(prefix.length(), other.length());
        return createOrSame(build(null, null, null, -1, rest, that.query, that.fragment, false));
    }

    /**
     * This same URI, checking that its authority has server form (user, host, port).
     *
     * <p>It exists because a URI's authority may be anything --the RFC leaves a "registry-based"
     * form for odd schemes-- and whoever needs a host and a port wants to fail early if there are
     * none.
     *
     * @throws URISyntaxException if the authority does not have server form
     */
    public URI parseServerAuthority() throws URISyntaxException {
        if (this.authority == null) {
            return this;
        }
        if (this.getHost() == null) {
            throw new URISyntaxException(this.string, "the authority does not have server form");
        }
        return this;
    }

    /**
     * This URI as a {@link java.net.URL}.
     *
     * @throws IllegalArgumentException if the URI is not absolute
     * @throws java.net.MalformedURLException if the scheme cannot be turned into a URL
     */
    public java.net.URL toURL() throws java.net.MalformedURLException {
        if (!this.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        return new java.net.URL(this.string);
    }

    private static boolean same(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // Every `resolve`/`relativize`/`normalize` produces text that **already** is a valid URI --it
    // comes out of pieces that were-- so a parse failure here would be a defect of this class and
    // not of the caller. That is why it is relabelled as `IllegalArgumentException`, just like
    // `URI.create`.
    private static URI createOrSame(String text) {
        return URI.create(text);
    }

    public String getPath() { return unescape(this.path); }

    public String getRawPath() { return this.path; }

    public String getQuery() { return unescape(this.query); }

    public String getRawQuery() { return this.query; }

    public String getFragment() { return unescape(this.fragment); }

    public String getRawFragment() { return this.fragment; }

    public String toString() { return this.string; }

    /**
     * The URI written with **ASCII only**.
     *
     * <p>Non-ASCII characters are kept literal --`toString()` of a URI with an accented "e" shows
     * it accented, and the JDK does the same--; this is the form to be sent over a channel that
     * only accepts ASCII, and it is the only difference between the two methods.
     */
    public String toASCIIString() {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int len = this.string.length();
        while (i < len) {
            char c = this.string.charAt(i);
            if (c < 0x80) {
                out.append(c);
                i = i + 1;
                continue;
            }
            // A supplementary character is TWO `char`s, and the whole pair has to be taken before
            // passing it to UTF-8: encoding each surrogate separately gives a sequence that does
            // not come back.
            int cp = this.string.codePointAt(i);
            byte[] bytes = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
            int k = 0;
            while (k < bytes.length) {
                escapeByte(out, bytes[k] & 0xFF);
                k = k + 1;
            }
            i = i + Character.charCount(cp);
        }
        return out.toString();
    }

    public boolean equals(Object other) {
        if (other == this) { return true; }
        if (other == null) { return false; }
        if (!(other instanceof URI)) { return false; }
        URI that = (URI) other;
        return this.string.equals(that.string);
    }

    public int hashCode() { return this.string.hashCode(); }

    public int compareTo(URI that) { return this.string.compareTo(that.string); }

    // ---- parsing helpers (our String has no indexOf) ----

    // The first `ch` in [from, to), or -1.
    private static int scanFor(String s, int from, int to, char ch) {
        int i = from;
        while (i < to) {
            if (s.charAt(i) == ch) { return i; }
            i = i + 1;
        }
        return -1;
    }

    private static boolean isSchemeStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isSchemeChar(char c) {
        return isSchemeStart(c) || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
    }

    // ---- percent-encoding (RFC 3986) --------------------------------------------------------------
    //
    // What gets escaped depends on the COMPONENT, and that is why there are four sets and not one:
    // a '?' inside a path has to be escaped --otherwise it cuts the path short and starts the
    // query-- but inside the query it is just another character and leaving it is correct. Escaping
    // too much is an error too: it changes the component's value.
    //
    // The sets are the JDK's, which uses RFC 2396's definition of "unreserved" --it includes the
    // "mark" characters !~*'()-- and not RFC 3986's shorter one. Verified against the real JDK.
    //
    // What is NEVER escaped here: the non-ASCII characters. The JDK leaves them literal in
    // `toString()` and in `getRawPath()`, and encodes them only in `toASCIIString()`.

    private static final String UNRESERVED =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    /** Legal in a path: the unreserved ones, plus the separators a path may contain. */
    private static final String LEGAL_PATH = UNRESERVED + ":@&=+$,/";

    /** Legal in a query or a fragment: there '/' and '?' no longer separate anything. */
    private static final String LEGAL_URIC = UNRESERVED + ";/?:@&=+$,[]";

    /** Legal in an authority; the brackets belong to literal IPv6 addresses. */
    private static final String LEGAL_AUTHORITY = UNRESERVED + "$,;:@&=+[]";

    /** Legal in the user part: it carries no '@', which is precisely what terminates it. */
    private static final String LEGAL_USERINFO = UNRESERVED + ";:&=+$,";

    private static final String HEX = "0123456789ABCDEF";

    // Escapes whatever is not legal in that component.
    private static String escape(String s, String legal) {
        if (s == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= 0x80 || scanFor(legal, 0, legal.length(), c) >= 0) {
                out.append(c);
            } else {
                escapeByte(out, c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    // In upper case, which is the RFC's canonical form and the one the JDK emits.
    private static void escapeByte(StringBuilder out, int b) {
        out.append('%');
        out.append(HEX.charAt((b >> 4) & 0xF));
        out.append(HEX.charAt(b & 0xF));
    }

    // It undoes the %XX. A malformed escape ("%zz") is left as it stands instead of throwing: this
    // method is called by the `getX()`, which declare no exception, and losing the rest of the
    // component over a stray '%' would be worse than returning it as it came.
    private static String unescape(String s) {
        if (s == null || scanFor(s, 0, s.length(), '%') < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder();
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int i = 0;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < len) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    // Consecutive %XX are gathered before decoding: a non-ASCII character is
                    // several bytes in UTF-8, and passing them one at a time would give one broken
                    // character per byte.
                    bytes.write((hi << 4) + lo);
                    i = i + 3;
                    continue;
                }
            }
            dumpBytes(out, bytes);
            out.append(c);
            i = i + 1;
        }
        dumpBytes(out, bytes);
        return out.toString();
    }

    private static void dumpBytes(StringBuilder out, java.io.ByteArrayOutputStream bytes) {
        if (bytes.size() == 0) {
            return;
        }
        out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        bytes.reset();
    }
}
