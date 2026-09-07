package java.net;

import java.io.Serializable;

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
// The earlier note said that `openConnection()`, `getContent()` and the handler machinery were left
// out because "there is no IO layer, no protocol handler, and no socket for them to work over". All
// three changed: `java.io` works, `URLConnection` is complete --its only abstract method is
// `connect()`-- and `URLStreamHandler` exists. What was missing was not the layer but the **seam**:
// whoever chooses a protocol's handler. That is what is added here.
//
// **What works and what does not, plainly.** `file:` works end to end: it has its own handler and
// reads from disk. Any other protocol works **if the program registers a handler**, through
// `setURLStreamHandlerFactory` or by passing one to the constructor. And if nobody registered one,
// `openConnection` throws an `IOException` saying `unknown protocol`, which is exactly what the JDK
// says.
//
// That last part is what keeps the method from being "a member that exists in order to fail", which
// was the old note's argument and was a good one while no handler was possible. With one that works
// and a way to bring more, `openConnection` is a method that does its job and says so when the
// protocol is not covered.
//
// A missing member is a legal subset; a member that lies is not.
public final class URL implements Serializable {

    private final URI uri;
    private final String spec;

    /**
     * The handler this URL uses, if it was given an explicit one at construction.
     *
     * <p>`null` means "whichever suits the protocol when it is needed", which is resolved late and
     * not at construction. Resolving it early would force the constructor to fail for a protocol with
     * no handler --which is what the JDK does-- and here that would break the whole use of `URL` as a
     * plain carrier of an address, which is what it is most used for.
     */
    private final URLStreamHandler handler;

    // The factory the program registered, or `null`. It can be set **once only**, just as in the
    // JDK: two libraries setting it would trample each other, and the second would change the meaning
    // of the URLs the first had already created.
    private static URLStreamHandlerFactory fabrica;

    // The `file:` handler, one only and shared: it has no state.
    private static final URLStreamHandler ARCHIVO = new KajiFileHandler();

    /**
     * Sets the program's handler factory.
     *
     * @throws Error if one had already been set
     */
    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        synchronized (URL.class) {
            if (fabrica != null) {
                throw new Error("factory already defined");
            }
            fabrica = fac;
        }
    }

    // A protocol's handler: this URL's explicit one, whatever the factory says, or `file:`'s.
    // `null` = there is none, and whoever asks decides what to do about it.
    private URLStreamHandler handlerDelProtocolo() {
        if (this.handler != null) {
            return this.handler;
        }
        URLStreamHandlerFactory f;
        synchronized (URL.class) {
            f = fabrica;
        }
        String protocolo = this.getProtocol();
        if (f != null) {
            URLStreamHandler h = f.createURLStreamHandler(protocolo);
            if (h != null) {
                return h;
            }
        }
        return "file".equalsIgnoreCase(protocolo) ? ARCHIVO : null;
    }

    /**
     * Parses {@code spec}. A URL must be absolute -- it names a resource, not a reference to one --
     * so a relative string is rejected, which is what makes this different from a URI.
     *
     * @throws MalformedURLException if the string does not parse, or has no protocol
     */
    public URL(String spec) throws MalformedURLException {
        this(spec, (URLStreamHandler) null);
    }

    // The only constructor that assigns: every other one ends up here. Having a single one is what
    // guarantees the `handler` is not forgotten in some variant.
    private URL(String spec, URLStreamHandler handler) throws MalformedURLException {
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
        this.handler = handler;
    }

    /**
     * A URL with a handler **of its own**, different from the one its protocol would get.
     *
     * <p>It is what allows speaking a protocol nobody registered globally, or speaking a known one in
     * another way, without touching the program's factory -- which is set once and belongs to
     * everyone.
     *
     * @deprecated the JDK has marked it so since Java 20 and recommends
     *             {@link #of(URI, URLStreamHandler)}, which separates parsing from construction
     */
    @Deprecated
    public URL(String protocol, String host, int port, String file, URLStreamHandler handler)
            throws MalformedURLException {
        this(buildSpec(protocol, host, port, file), handler);
    }

    /**
     * The {@link #URL(URL, String)} one with a handler of its own.
     *
     * @deprecated the same as the one above
     */
    @Deprecated
    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        this(resolverContra(context, spec), handler);
    }

    /** A URL from a `URI` with a handler of its own. */
    public static URL of(URI uri, URLStreamHandler handler) throws MalformedURLException {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        return new URL(uri.toString(), handler);
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

    /**
     * Resolves `spec` against `context`, which acts as the base.
     *
     * <p>It is the constructor that makes relative links useful: `new URL(page, "../img/a.png")` is
     * what a browser does with every `href`. The resolution is RFC 3986's and lives in
     * {@link java.net.URI#resolve}, so that the two classes cannot disagree.
     *
     * @throws MalformedURLException if the result is not a valid URL
     */
    public URL(URL context, String spec) throws MalformedURLException {
        this(resolverContra(context, spec));
    }

    private static String resolverContra(URL context, String spec) throws MalformedURLException {
        if (spec == null) {
            throw new MalformedURLException("null spec");
        }
        if (context == null) {
            return spec;
        }
        URI relativo;
        try {
            relativo = new URI(spec);
        } catch (URISyntaxException bad) {
            throw new MalformedURLException(bad.getMessage());
        }
        // Bound to a local: chaining through an intermediate is lost (#108).
        URI base = context.toURI();
        URI resuelto = base.resolve(relativo);
        return resuelto.toString();
    }

    /**
     * The port this protocol uses when none is written.
     *
     * <p>`-1` for a protocol whose default port this library does not know, which is what the JDK
     * returns for a protocol with no registered handler. The four that are known are the ones that
     * turn up in practice; inventing the rest would be worse than saying "I do not know".
     */
    public int getDefaultPort() {
        String p = this.getProtocol();
        if ("http".equals(p)) {
            return 80;
        }
        if ("https".equals(p)) {
            return 443;
        }
        if ("ftp".equals(p)) {
            return 21;
        }
        if ("file".equals(p)) {
            // `file:` has no port, and the JDK reports it that way -- not as "unknown".
            return -1;
        }
        return -1;
    }

    /**
     * Whether the two URLs name the **same resource**, ignoring the fragment.
     *
     * <p>That the fragment does not count is the whole point: `page#section1` and `page#section2` are
     * the same document fetched once, and a cache treating them as different would download it
     * twice.
     */
    public boolean sameFile(URL other) {
        if (other == null) {
            return false;
        }
        if (!igual(this.getProtocol(), other.getProtocol())) {
            return false;
        }
        if (!igual(this.getHost(), other.getHost())) {
            return false;
        }
        if (this.puertoEfectivo() != other.puertoEfectivo()) {
            return false;
        }
        return igual(this.getFile(), other.getFile());
    }

    // The written port, or the protocol's: `http://a` and `http://a:80` are the same resource.
    private int puertoEfectivo() {
        int p = this.getPort();
        if (p == -1) {
            return this.getDefaultPort();
        }
        return p;
    }

    private static boolean igual(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    // The accessors below ask `URI` for the **raw** forms on purpose: the JDK's `URL.getPath()`
    // returns the path as it appears in the URL, without decoding the %XX, and whoever wants the
    // decoded value goes through `toURI().getPath()`. Delegating to `URI.getPath()` --which does
    // decode-- would make `new URL("file:/a%20b").getPath()` return "/a b", and there the URL would
    // stop being reconstructible from its parts.

    /** The scheme: {@code http}, {@code file}, {@code jar}. */
    public String getProtocol() {
        return this.uri.getScheme();
    }

    /**
     * The host, or the empty string if the URL has no authority.
     *
     * <p>Empty and not `null`, which is what the JDK returns: an opaque URL --`jar:`, `mailto:`-- has
     * no host, and `URL` represents that with `""`. `URI` uses `null` for the same thing; the
     * difference belongs to the two classes and not to this implementation.
     */
    public String getHost() {
        String h = this.uri.getHost();
        if (h == null && this.uri.isOpaque()) {
            return "";
        }
        return h;
    }

    /** The port, or -1 if the URL did not give one. */
    public int getPort() {
        return this.uri.getPort();
    }

    /**
     * The URL's path.
     *
     * <p>For an **opaque** URL --one whose scheme-specific part does not start with `/`, like
     * `jar:file:/x/a.jar!/e.txt` or `mailto:a@b`-- it returns the whole scheme-specific part. `URI`
     * calls it something else and its `getRawPath()` gives `null` there, but `URL` does not tell the
     * two apart: the JDK returns `file:/x/a.jar!/e.txt` for that URL, and that is where
     * `JarURLConnection` gets the `!/` that splits the `.jar` from its entry.
     *
     * <p>It used to return `null` for every opaque URL, and that broke `JarURLConnection` on its
     * first line: with no path there is no `!/` to find.
     */
    public String getPath() {
        String p = this.uri.getRawPath();
        if (p == null && this.uri.isOpaque()) {
            return this.uri.getRawSchemeSpecificPart();
        }
        return p;
    }

    public String getQuery() {
        return this.uri.getRawQuery();
    }

    public String getRef() {
        return this.uri.getRawFragment();
    }

    public String getUserInfo() {
        return this.uri.getRawUserInfo();
    }

    public String getAuthority() {
        return this.uri.getRawAuthority();
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

    /**
     * Opens a stream to read this URL's content.
     *
     * <p>**`file:` only.** It is the one scheme this library can serve: reading from `http:` asks for
     * an HTTP client, and `jar:` asks for reading a nested ZIP -- both are machinery that is not
     * here. For any other scheme it throws, with the same type of exception the JDK uses when it has
     * no handler for the protocol.
     *
     * <p>The JDK does this in two steps --`openConnection().getInputStream()`-- because it has a
     * `URLConnection` hierarchy per protocol. Here there is only one, so the intermediate step would
     * add nothing but a layer.
     *
     * @throws java.io.IOException if the scheme is not `file:`, or if the file cannot be read
     */
    public final java.io.InputStream openStream() throws java.io.IOException {
        String scheme = this.getProtocol();
        if (!"file".equals(scheme)) {
            throw new java.net.UnknownServiceException(
                    "this library can only open file: URLs, not " + scheme + ":");
        }
        String ruta = this.getPath();
        if (ruta == null || ruta.length() == 0) {
            throw new java.io.IOException("the URL has no path: " + this.spec);
        }
        // A Windows path arrives as `/C:/x`: the extra slash belongs to the URL's format, not to the
        // file system, and it has to come off before touching the disk.
        if (ruta.length() > 2 && ruta.charAt(0) == '/' && ruta.charAt(2) == ':') {
            ruta = ruta.substring(1);
        }
        return new java.io.FileInputStream(ruta);
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

    // ---- fetching the resource -----------------------------------------------------------------
    //
    // The half of a URL that **brings** what the address names. All four hang off the handler, and the
    // handler exists for `file:` always and for the rest if the program registered one.

    /**
     * A connection to the resource this URL names.
     *
     * <p>It does not connect: it returns the object the connection is configured on and then
     * connected. That separation is what allows timeouts and headers to be set before anything is
     * touched.
     *
     * @throws IOException if the protocol has no handler, or if building it fails
     */
    public URLConnection openConnection() throws java.io.IOException {
        URLStreamHandler h = this.handlerDelProtocolo();
        if (h == null) {
            throw new java.io.IOException("unknown protocol: " + this.getProtocol());
        }
        return h.openConnection(this);
    }

    /**
     * The one above through a proxy.
     *
     * <p>The proxy is **accepted and ignored**, and that has to be said: a handler the program
     * registers may honour it if it wants; this class has no way of passing it on, because
     * `URLStreamHandler.openConnection(URL, Proxy)` is `protected` and its proxy-less version is the
     * only abstract one. The one handler that comes installed --`file:`-- crosses no network anyway.
     *
     * @throws IllegalArgumentException if `proxy` is null
     */
    public URLConnection openConnection(Proxy proxy) throws java.io.IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy can not be null");
        }
        return this.openConnection();
    }

    /** The resource's content, of whichever type the handler decides. */
    public final Object getContent() throws java.io.IOException {
        return this.openConnection().getContent();
    }

    /** The content converted to the first of those types it can be. */
    public final Object getContent(Class<?>[] classes) throws java.io.IOException {
        return this.openConnection().getContent(classes);
    }
}
