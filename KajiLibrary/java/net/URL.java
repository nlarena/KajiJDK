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
// La nota anterior decia que `openConnection()`, `getContent()` y la maquinaria de handlers quedaban
// afuera porque "no hay capa de IO, ni handler de protocolo, ni socket para que funcionen". Las tres
// cosas cambiaron: `java.io` anda, `URLConnection` esta entera --su unico metodo abstracto es
// `connect()`-- y `URLStreamHandler` existe. Lo que faltaba no era la capa sino la **costura**: quien
// elige el handler de un protocolo. Es lo que se agrega aca.
//
// **Que anda y que no, sin vueltas.** `file:` anda de punta a punta: tiene handler propio y lee del
// disco. Cualquier otro protocolo anda **si el programa registra un handler**, por
// `setURLStreamHandlerFactory` o pasandolo al constructor. Y si nadie lo registro, `openConnection`
// tira un `IOException` que dice `unknown protocol`, que es exactamente lo que el JDK dice.
//
// Esa ultima parte es la que hace que el metodo no sea "un miembro que existe para fallar", que era
// el argumento de la nota vieja y era bueno mientras no hubiera ningun handler posible. Con uno que
// funciona y una via para traer mas, `openConnection` es un metodo que hace su trabajo y avisa
// cuando el protocolo no esta cubierto.
//
// A missing member is a legal subset; a member that lies is not.
public final class URL {

    private final URI uri;
    private final String spec;

    /**
     * El handler que esta URL usa, si se le dio uno explicito al construirla.
     *
     * <p>`null` significa "el que corresponda al protocolo cuando haga falta", que se resuelve tarde
     * y no al construir. Resolverlo temprano obligaria a que el constructor fallara para un protocolo
     * sin handler --que es lo que hace el JDK-- y aca eso romperia todo el uso de `URL` como simple
     * portadora de una direccion, que es para lo que mas se la usa.
     */
    private final URLStreamHandler handler;

    // La fabrica que el programa registro, o `null`. Se puede fijar **una sola vez**, igual que en el
    // JDK: dos librerias que la fijaran se pisarian, y la segunda cambiaria el significado de las URL
    // que la primera ya creo.
    private static URLStreamHandlerFactory fabrica;

    // El handler de `file:`, uno solo y compartido: no tiene estado.
    private static final URLStreamHandler ARCHIVO = new KajiFileHandler();

    /**
     * Fija la fabrica de handlers del programa.
     *
     * @throws Error si ya se habia fijado una
     */
    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        synchronized (URL.class) {
            if (fabrica != null) {
                throw new Error("factory already defined");
            }
            fabrica = fac;
        }
    }

    // El handler de un protocolo: el explicito de esta URL, el que diga la fabrica, o el de `file:`.
    // `null` = no hay ninguno, y quien pregunte decide que hacer con eso.
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

    // El unico constructor que asigna: todos los demas terminan aca. Tener uno solo es lo que
    // garantiza que el `handler` no se olvide en alguna variante.
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
     * Una URL con un handler **propio**, distinto del que le tocaria a su protocolo.
     *
     * <p>Es lo que permite hablar un protocolo que nadie registro globalmente, o hablar uno conocido
     * de otra manera, sin tocar la fabrica del programa -- que se fija una sola vez y es de todos.
     *
     * @deprecated el JDK lo marca asi desde Java 20 y recomienda {@link #of(URI, URLStreamHandler)},
     *             que separa el parseo de la construccion
     */
    @Deprecated
    public URL(String protocol, String host, int port, String file, URLStreamHandler handler)
            throws MalformedURLException {
        this(buildSpec(protocol, host, port, file), handler);
    }

    /**
     * El de {@link #URL(URL, String)} con un handler propio.
     *
     * @deprecated igual que el de arriba
     */
    @Deprecated
    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        this(resolverContra(context, spec), handler);
    }

    /** Una URL desde un `URI` con un handler propio. */
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
     * Resuelve `spec` contra `context`, que hace de base.
     *
     * <p>Es el constructor que hace utiles a los enlaces relativos: `new URL(pagina, "../img/a.png")`
     * es lo que un navegador hace con cada `href`. La resolucion es la del RFC 3986 y vive en
     * {@link java.net.URI#resolve}, para que las dos clases no puedan discrepar.
     *
     * @throws MalformedURLException si el resultado no es una URL valida
     */
    public URL(URL context, String spec) throws MalformedURLException {
        this(resolverContra(context, spec));
    }

    private static String resolverContra(URL context, String spec) throws MalformedURLException {
        if (spec == null) {
            throw new MalformedURLException("spec nulo");
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
        // Ligado a una local: encadenar por un intermedio se pierde (#108).
        URI base = context.toURI();
        URI resuelto = base.resolve(relativo);
        return resuelto.toString();
    }

    /**
     * El puerto que este protocolo usa cuando no se escribe uno.
     *
     * <p>`-1` para un protocolo cuyo puerto por defecto esta biblioteca no conoce, que es lo que el
     * JDK devuelve para un protocolo sin manejador registrado. Los cuatro que se conocen son los que
     * aparecen en la practica; inventar los demas seria peor que decir "no se".
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
            // `file:` no tiene puerto, y el JDK lo reporta asi -- no como "desconocido".
            return -1;
        }
        return -1;
    }

    /**
     * Si las dos URLs nombran el **mismo recurso**, ignorando el fragmento.
     *
     * <p>Que el fragmento no cuente es todo el punto: `pagina#seccion1` y `pagina#seccion2` son el
     * mismo documento traido una sola vez, y un cache que las tratara como distintas lo bajaria dos
     * veces.
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

    // El puerto escrito, o el del protocolo: `http://a` y `http://a:80` son el mismo recurso.
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

    // Los accesores de abajo piden las formas **crudas** de `URI` a proposito: `URL.getPath()` del
    // JDK devuelve el camino tal como aparece en la URL, sin decodificar los %XX, y quien quiera el
    // valor decodificado pasa por `toURI().getPath()`. Delegar en `URI.getPath()` --que si
    // decodifica-- haria que `new URL("file:/a%20b").getPath()` devolviera "/a b", y ahi la URL
    // dejaria de poder reconstruirse a partir de sus partes.

    /** The scheme: {@code http}, {@code file}, {@code jar}. */
    public String getProtocol() {
        return this.uri.getScheme();
    }

    /**
     * El host, o la cadena vacia si la URL no tiene autoridad.
     *
     * <p>Vacia y no `null`, que es lo que devuelve el JDK: una URL opaca --`jar:`, `mailto:`-- no
     * tiene host, y `URL` lo representa con `""`. `URI` usa `null` para lo mismo; la diferencia es
     * de las dos clases y no de esta implementacion.
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
     * La ruta de la URL.
     *
     * <p>Para una URL **opaca** --una cuya parte especifica no empieza con `/`, como
     * `jar:file:/x/a.jar!/e.txt` o `mailto:a@b`-- devuelve la parte especifica entera. `URI` la
     * llama de otra forma y su `getRawPath()` da `null` ahi, pero `URL` no distingue las dos cosas:
     * el JDK devuelve `file:/x/a.jar!/e.txt` para esa URL, y de ahi es de donde `JarURLConnection`
     * saca el `!/` que parte al `.jar` de su entrada.
     *
     * <p>Devolvia `null` para toda URL opaca, y eso rompia a `JarURLConnection` en la primera linea:
     * sin ruta no hay `!/` que encontrar.
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
     * Abre un flujo para leer el contenido de esta URL.
     *
     * <p>**Solo `file:`.** Es el unico esquema que esta biblioteca puede atender: leer de `http:`
     * pide un cliente HTTP, y `jar:` pide leer un ZIP anidado -- las dos cosas son maquinaria que no
     * esta. Para cualquier otro esquema tira, con el mismo tipo de excepcion que el JDK usa cuando no
     * tiene un manejador para el protocolo.
     *
     * <p>El JDK hace esto en dos pasos --`openConnection().getInputStream()`-- porque tiene una
     * jerarquia de `URLConnection` por protocolo. Aca hay uno solo, asi que el paso intermedio no
     * agregaria mas que una capa.
     *
     * @throws java.io.IOException si el esquema no es `file:`, o si el archivo no se puede leer
     */
    public final java.io.InputStream openStream() throws java.io.IOException {
        String esquema = this.getProtocol();
        if (!"file".equals(esquema)) {
            throw new java.net.UnknownServiceException(
                    "esta biblioteca solo sabe abrir URLs file:, no " + esquema + ":");
        }
        String ruta = this.getPath();
        if (ruta == null || ruta.length() == 0) {
            throw new java.io.IOException("la URL no tiene ruta: " + this.spec);
        }
        // Una ruta de Windows llega como `/C:/x`: la barra de mas es del formato de la URL, no del
        // sistema de archivos, y hay que sacarla antes de tocar el disco.
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

    // ---- recuperar el recurso -----------------------------------------------------------------
    //
    // La mitad de una URL que **trae** lo que la direccion nombra. Los cuatro cuelgan del handler, y
    // el handler existe para `file:` siempre y para lo demas si el programa lo registro.

    /**
     * Una conexion al recurso que esta URL nombra.
     *
     * <p>No conecta: devuelve el objeto con el que se configura la conexion y despues se conecta.
     * Esa separacion es la que deja fijar tiempos de espera y cabeceras antes de tocar nada.
     *
     * @throws IOException si el protocolo no tiene handler, o si armarla falla
     */
    public URLConnection openConnection() throws java.io.IOException {
        URLStreamHandler h = this.handlerDelProtocolo();
        if (h == null) {
            throw new java.io.IOException("unknown protocol: " + this.getProtocol());
        }
        return h.openConnection(this);
    }

    /**
     * La de arriba a traves de un proxy.
     *
     * <p>El proxy se **acepta y se ignora**, y eso hay que decirlo: no hay socket en esta VM, asi que
     * ningun handler propio puede estar hablando por TCP, y el unico que viene puesto --`file:`-- no
     * atraviesa ninguna red. Un handler que el programa registre puede honrarlo si quiere; esta clase
     * no tiene como pasarselo, porque `URLStreamHandler.openConnection(URL, Proxy)` es `protected` y
     * su version sin proxy es la unica abstracta.
     *
     * @throws IllegalArgumentException si `proxy` es null
     */
    public URLConnection openConnection(Proxy proxy) throws java.io.IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy can not be null");
        }
        return this.openConnection();
    }

    /** El contenido del recurso, del tipo que el handler decida. */
    public final Object getContent() throws java.io.IOException {
        return this.openConnection().getContent();
    }

    /** El contenido convertido al primero de esos tipos que se pueda. */
    public final Object getContent(Class<?>[] classes) throws java.io.IOException {
        return this.openConnection().getContent(classes);
    }
}
