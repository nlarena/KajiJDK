package java.net;

// KajiLibrary's java.net.URL (finding #267).
//
// It exists because `jakarta.persistence.spi.PersistenceUnitInfo` returns them
// (`getPersistenceUnitRootUrl()`, `getJarFileUrls()`), and without the class the file does not
// compile.
//
// The parsing is DELEGATED to java.net.URI, which is already here and already implements RFC 3986.
// Two parsers for the same grammar is one more chance for them to disagree, and a URL that split a
// string differently from the URI built out of the same string would be a bug nobody would think
// to look for.
//
// What it deliberately does NOT have: `openStream()`, `openConnection()`, `getContent()` and the
// URLStreamHandler machinery. Those are the half of a URL that RETRIEVES, and there is no IO layer,
// no protocol handler and no socket here for them to work through. A `openStream()` that threw on
// every call would be a member that exists to fail; leaving it out says the same thing without
// pretending.
//
// Also absent: `setURLStreamHandlerFactory`, and the deprecated constructors that take a handler.
//
// A missing member is a legal subset; a member that lies is not.
public final class URL {

    private final URI uri;
    private final String spec;

    /**
     * Parses {@code spec}. A URL must be absolute -- it names a resource, not a reference to one --
     * so a relative string is rejected, which is what makes this different from a URI.
     *
     * @throws MalformedURLException if the string does not parse, or has no protocol
     */
    public URL(String spec) throws MalformedURLException {
        URI parsed;
        try {
            parsed = new URI(spec);
        } catch (URISyntaxException bad) {
            throw new MalformedURLException(bad.getMessage());
        }
        if (parsed.getScheme() == null) {
            throw new MalformedURLException("no protocol: " + spec);
        }
        this.uri = parsed;
        this.spec = spec;
    }

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(buildSpec(protocol, host, port, file));
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file);
    }

    private static String buildSpec(String protocol, String host, int port, String file) {
        StringBuilder out = new StringBuilder(protocol);
        out.append("://");
        if (host != null) {
            out.append(host);
        }
        if (port != -1) {
            out.append(':');
            out.append(port);
        }
        if (file != null) {
            out.append(file);
        }
        return out.toString();
    }

    /** The scheme: {@code http}, {@code file}, {@code jar}. */
    public String getProtocol() {
        return this.uri.getScheme();
    }

    public String getHost() {
        return this.uri.getHost();
    }

    /** The port, or -1 if the URL did not give one. */
    public int getPort() {
        return this.uri.getPort();
    }

    public String getPath() {
        return this.uri.getPath();
    }

    public String getQuery() {
        return this.uri.getQuery();
    }

    public String getRef() {
        return this.uri.getFragment();
    }

    public String getUserInfo() {
        return this.uri.getUserInfo();
    }

    public String getAuthority() {
        return this.uri.getAuthority();
    }

    /** Path plus query -- the part after the authority, which is what a request line carries. */
    public String getFile() {
        String path = this.getPath();
        if (path == null) {
            path = "";
        }
        String query = this.getQuery();
        if (query == null) {
            return path;
        }
        return path + "?" + query;
    }

    /** The same resource as a {@link URI}. Always succeeds: the URI is what parsed it. */
    public URI toURI() {
        return this.uri;
    }

    public String toExternalForm() {
        return this.spec;
    }

    @Override
    public String toString() {
        return this.spec;
    }

    /**
     * Compares the PARSED form, not the text, so two spellings of the same resource are equal.
     *
     * <p>Unlike the JDK's, this does NOT resolve host names: {@code URL.equals} there is
     * famously blocking, because it compares the IP addresses the two hosts resolve to. There is
     * no resolver here, and inheriting that surprise would be inheriting the worst part of the
     * class.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof URL && this.uri.equals(((URL) other).uri);
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }
}
