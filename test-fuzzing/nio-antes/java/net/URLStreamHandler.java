package java.net;

import java.io.IOException;

// The one that knows how to speak ONE protocol: how a URL of its own is written, and how it is
// opened.
//
// It is the piece that makes `java.net.URL` extensible. `URL` knows nothing about `http` or `file`:
// it delegates to the scheme's handler, and that is why adding a new protocol means writing one of
// these and registering it with a `URLStreamHandlerFactory`.
//
// ===========================================================================================
// WHAT IT DOES AND WHAT IT DOES NOT DO
// ===========================================================================================
//
// The half that **compares and writes** URLs is here, which is pure computation over the already
// separated parts: `equals`, `hashCode`, `sameFile`, `hostsEqual`, `toExternalForm`,
// `getDefaultPort`. They are complete and use the JDK's own algorithm.
//
// `openConnection(URL)` is left **abstract**, as in the JDK: it is the method the whole transport
// hangs off, and declaring it abstract promises nothing -- whoever implements a protocol writes it.
// `openConnection(URL, Proxy)` throws `UnsupportedOperationException`, which is **literally what the
// JDK's base class does** ("Method not implemented."): a handler that knows nothing about proxies
// does not have to.
//
// THREE MEMBERS DECLINE, and the reason is the same for all three: `parseURL(URL,String,int,int)`
// and the two `setURL` overloads. They exist only to **mutate a `URL`'s internal fields** -- they are
// the only road by which the JDK lets a third party write inside an already built URL.
//
// This tree's `java.net.URL` is **immutable**: it keeps the `URI` that parsed it and nothing else,
// and its whole decomposition comes from there (`URL.java`'s header explains why parsing was
// delegated to `URI` instead of having two parsers of the same grammar). There are no fields to
// write, and adding them so that these three methods could work would be breaking the immutability of
// a class used everywhere in order to improve a count by three.
//
// The consequence has to be said straight: **a `URLStreamHandler` written here cannot parse a scheme
// with a syntax of its own**. It can compare them, write them and open them; the parsing is done by
// `URI` for everyone alike. The three methods are declared with their exact signatures --a subclass
// that overrides them compiles-- and what declines is the inherited version, which would be lying if
// it did nothing.
//
// `getHostAddress` is here, and always returns null: its contract is "the host's address, or null if
// it is not known", and here it is not known because there is no resolver (see `InetAddress`'s
// header). Null is the true answer, not a plug -- and the JDK returns null in exactly the same way
// when the name does not resolve.
public abstract class URLStreamHandler {

    public URLStreamHandler() {
    }

    /**
     * Opens the connection to {@code u}.
     *
     * <p>Abstract: it is the one thing this class cannot know on its own.
     */
    protected abstract URLConnection openConnection(URL u) throws IOException;

    /**
     * Opens the connection to {@code u} going out through {@code p}.
     *
     * <p>The base class **does not support it and says so**, with this same text, in the JDK: a
     * handler that knows how to use proxies overrides it.
     *
     * @throws UnsupportedOperationException always, in the base class
     */
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    /**
     * The port used when the URL names none, or -1 if the protocol has none.
     *
     * <p>-1 in the base class: a generic protocol has no port by convention.
     */
    protected int getDefaultPort() {
        return -1;
    }

    /**
     * Whether the two URLs name the same resource, fragment included.
     *
     * <p>It is {@link #sameFile} plus the fragment comparison. That the fragment counts here and not
     * in `sameFile` is the difference between the two: `#section2` is another part of the same file.
     */
    protected boolean equals(URL u1, URL u2) {
        String ref1 = u1.getRef();
        String ref2 = u2.getRef();
        boolean sameRef = ref1 == null ? ref2 == null : ref1.equals(ref2);
        return sameRef && this.sameFile(u1, u2);
    }

    /**
     * The hash matching {@link #equals(URL, URL)}: the sum of the parts.
     *
     * <p>A sum and not a positional combination, just like the JDK. The host goes in lower case,
     * because a host name is case-insensitive and two spellings of the same host have to give the
     * same number.
     */
    protected int hashCode(URL u) {
        int h = 0;
        String protocol = u.getProtocol();
        if (protocol != null) {
            h = h + protocol.hashCode();
        }
        InetAddress addr = this.getHostAddress(u);
        if (addr != null) {
            h = h + addr.hashCode();
        } else {
            String host = u.getHost();
            if (host != null) {
                h = h + host.toLowerCase().hashCode();
            }
        }
        String file = u.getFile();
        if (file != null) {
            h = h + file.hashCode();
        }
        h = h + (u.getPort() == -1 ? this.getDefaultPort() : u.getPort());
        String ref = u.getRef();
        if (ref != null) {
            h = h + ref.hashCode();
        }
        return h;
    }

    /**
     * Whether the two URLs name the same file, **without** looking at the fragment.
     *
     * <p>It compares protocol, host, effective port and path. The "effective" port is the one the URL
     * carries or, failing that, the protocol's: that is why {@code http://x/} and {@code http://x:80/}
     * are the same file.
     */
    protected boolean sameFile(URL u1, URL u2) {
        String p1 = u1.getProtocol();
        String p2 = u2.getProtocol();
        if (!(p1 == null ? p2 == null : p1.equalsIgnoreCase(p2))) {
            return false;
        }
        String f1 = u1.getFile();
        String f2 = u2.getFile();
        if (!(f1 == null ? f2 == null : f1.equals(f2))) {
            return false;
        }
        int port1 = u1.getPort() == -1 ? this.getDefaultPort() : u1.getPort();
        int port2 = u2.getPort() == -1 ? this.getDefaultPort() : u2.getPort();
        if (port1 != port2) {
            return false;
        }
        return this.hostsEqual(u1, u2);
    }

    /**
     * Whether the two URLs point at the same host.
     *
     * <p>The JDK compares the IP addresses first --so that a name and its IP come out the same host--
     * and only if one of them does not resolve does it compare the names. Here {@link #getHostAddress}
     * always gives null, so the comparison is always by name, case-insensitively.
     *
     * <p>That makes this comparison **stricter** than the JDK's, never laxer: it may say two URLs are
     * of different hosts where the JDK would say they are the same, and not the other way round.
     */
    protected boolean hostsEqual(URL u1, URL u2) {
        InetAddress a1 = this.getHostAddress(u1);
        InetAddress a2 = this.getHostAddress(u2);
        if (a1 != null && a2 != null) {
            return a1.equals(a2);
        }
        String h1 = u1.getHost();
        String h2 = u2.getHost();
        if (h1 != null && h2 != null) {
            return h1.equalsIgnoreCase(h2);
        }
        return h1 == null && h2 == null;
    }

    /**
     * The IP address of {@code u}'s host, or null if it is not known.
     *
     * <p>Always null in KajiJDK: there is no name resolver. Null is an answer the contract already
     * allows for --the JDK gives it when the name does not resolve-- and this method's two callers,
     * {@link #hashCode} and {@link #hostsEqual}, have their alternative path written.
     */
    protected InetAddress getHostAddress(URL u) {
        return null;
    }

    /**
     * The URL written as text.
     *
     * <p>It reassembles {@code protocol://authority + file + #fragment}. The missing pieces are
     * omitted whole, separator included: a URL with no authority carries no double slash.
     */
    protected String toExternalForm(URL u) {
        StringBuilder b = new StringBuilder();
        b.append(u.getProtocol()).append(':');
        String authority = u.getAuthority();
        if (authority != null && authority.length() > 0) {
            b.append("//").append(authority);
        }
        String file = u.getFile();
        if (file != null) {
            b.append(file);
        }
        String ref = u.getRef();
        if (ref != null) {
            b.append('#').append(ref);
        }
        return b.toString();
    }

    /**
     * Parses {@code spec} and loads the components into {@code u}.
     *
     * <p>It is the protocol by which the JDK lets a handler understand a scheme of its own: the
     * {@link URL} arrives empty, the handler parses it its own way and fills it in with
     * {@link #setURL}.
     *
     * <h2>Why it cannot work here</h2>
     *
     * <p>Because this library's {@link URL} is <strong>immutable</strong>: it keeps a
     * {@link java.net.URI} and a string, both {@code final}. There is nothing to fill in after it is
     * built, so the "parse and load" protocol has nothing to rest on.
     *
     * <p>It is not an omission that writing more would fix: it would mean changing {@code URL}'s
     * representation. It is declared with the exact signature —a subclass that overrides it compiles—
     * and what declines is the inherited version, which would be lying if it did nothing.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    protected void parseURL(URL u, String spec, int start, int limit) {
        throw new UnsupportedOperationException(
                "this library's URL is immutable: it cannot be filled in after it is created");
    }

    /**
     * Loads the components into {@code u}.
     *
     * @throws UnsupportedOperationException always — see {@link #parseURL}
     */
    protected void setURL(URL u, String protocol, String host, int port, String authority,
            String userInfo, String path, String query, String ref) {
        throw new UnsupportedOperationException(
                "this library's URL is immutable: it cannot be filled in after it is created");
    }

    /**
     * The old form, from before a URL told authority and host apart.
     *
     * @deprecated use the nine-argument one, which separates {@code authority}, {@code userInfo} and
     *     {@code query} instead of stuffing them into {@code file}
     * @throws UnsupportedOperationException always — see {@link #parseURL}
     */
    @Deprecated(since = "1.2")
    protected void setURL(URL u, String protocol, String host, int port, String file, String ref) {
        throw new UnsupportedOperationException(
                "this library's URL is immutable: it cannot be filled in after it is created");
    }
}
