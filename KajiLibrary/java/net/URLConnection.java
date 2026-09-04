package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// El enlace entre una URL y el recurso que nombra, antes y despues de traerlo.
//
// La clase se lee mejor si se la parte en dos mitades, porque son dos cosas distintas pegadas:
//
//  1. **Configuracion**: todo lo que se setea ANTES de `connect()` --timeouts, `doInput`/`doOutput`,
//     cache, `If-Modified-Since`, propiedades de pedido--. Es un objeto de configuracion, y por eso
//     todos esos setters tiran `IllegalStateException` si ya se conecto: cambiarlos despues no
//     tendria efecto, y fallar es mejor que ignorar en silencio.
//
//  2. **Lectura del resultado**: `getHeaderField*`, `getContentType`, `getInputStream`. Depende de
//     que alguien haya hablado con el otro lado.
//
// ===========================================================================================
// QUE ENTRA Y QUE NO EN KajiJDK
// ===========================================================================================
//
// Entra la clase **entera**, y no es una concesion: `URLConnection` es **abstracta** y su unico
// metodo abstracto es `connect()`. Todo lo demas son implementaciones base que el JDK define, y las
// definiciones base son honestas de reproducir porque no prometen datos:
//
//  - `getHeaderFields()` devuelve el mapa vacio, `getHeaderField(String)` devuelve null. Es lo que
//    hace el JDK: una conexion base no tiene cabeceras, y las subclases que si las tienen las
//    sobreescriben. De ahi salen `getContentType`, `getDate`, `getExpiration` y compania.
//  - `getInputStream()` y `getOutputStream()` tiran `UnknownServiceException` con el mismo texto
//    que el JDK ("protocol doesn't support input"/"output"). **Eso no es un metodo que miente**: es
//    literalmente el contrato documentado del metodo en la clase base, no un stub. Quien escriba
//    una subclase que si sabe leer, la sobreescribe y anda.
//
// Lo que NO entra: nada. La unica pieza que queda coja es la resolucion de `ContentHandler`, que en
// el JDK barre paquetes buscando manejadores por tipo MIME; aca solo consulta la factoria que la
// aplicacion instale con `setContentHandlerFactory`. Sin factoria, `getContent()` termina en la
// `UnknownServiceException` de `getInputStream()`, que es exactamente donde termina en el JDK con
// una conexion base.
//
// `guessContentTypeFromName` y `guessContentTypeFromStream` son computo puro --una tabla de
// extensiones y unos numeros magicos-- y estan completas.
public abstract class URLConnection {

    /** La URL de este enlace. */
    protected URL url;

    /** Si se va a leer del recurso. Por defecto true. */
    protected boolean doInput = true;

    /** Si se va a escribir al recurso. Por defecto false. */
    protected boolean doOutput = false;

    /** Si se le puede preguntar cosas al usuario (un dialogo de contrasena, por ejemplo). */
    protected boolean allowUserInteraction = defaultAllowUserInteraction;

    /** Si se puede usar una copia cacheada. */
    protected boolean useCaches = true;

    /** Solo traerlo si cambio despues de este instante; 0 lo desactiva. */
    protected long ifModifiedSince = 0;

    /** Si ya se hablo con el otro lado. */
    protected boolean connected = false;

    private static boolean defaultAllowUserInteraction = false;
    private static volatile FileNameMap fileNameMap;
    private static ContentHandlerFactory contentHandlerFactory;
    private static final Map<String, Boolean> defaultUseCachesPorProtocolo =
            new HashMap<String, Boolean>();

    private int connectTimeout;
    private int readTimeout;

    // Se conservan en el orden en que se agregaron y con la caja con que se escribieron, pero se
    // buscan sin distinguir mayusculas: es lo que hace el JDK, y refleja que en HTTP el nombre de
    // una cabecera no distingue caja pero se manda como se escribio.
    private final Map<String, List<String>> requestProperties =
            new LinkedHashMap<String, List<String>>();

    /**
     * Un enlace a {@code url}, sin conectar.
     *
     * <p>Es {@code protected} porque nadie construye una `URLConnection` a mano: se la pide a una
     * `URL`, que elige la subclase segun el protocolo.
     */
    protected URLConnection(URL url) {
        this.url = url;
    }

    /**
     * Habla con el otro lado.
     *
     * <p>Abstracto a proposito y desde el JDK: es LA operacion que depende del protocolo, y no hay
     * una implementacion base que tenga sentido.
     */
    public abstract void connect() throws IOException;

    // ---- configuracion previa a la conexion ----

    /** Milisegundos que se espera a que la conexion se establezca; 0 es "para siempre". */
    public void setConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.connectTimeout = timeout;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    /** Milisegundos que una lectura espera datos; 0 es "para siempre". */
    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.readTimeout = timeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public URL getURL() {
        return this.url;
    }

    private void chequearNoConectado() {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
    }

    public void setDoInput(boolean doinput) {
        this.chequearNoConectado();
        this.doInput = doinput;
    }

    public boolean getDoInput() {
        return this.doInput;
    }

    public void setDoOutput(boolean dooutput) {
        this.chequearNoConectado();
        this.doOutput = dooutput;
    }

    public boolean getDoOutput() {
        return this.doOutput;
    }

    public void setAllowUserInteraction(boolean allowuserinteraction) {
        this.chequearNoConectado();
        this.allowUserInteraction = allowuserinteraction;
    }

    public boolean getAllowUserInteraction() {
        return this.allowUserInteraction;
    }

    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    public void setUseCaches(boolean usecaches) {
        this.chequearNoConectado();
        this.useCaches = usecaches;
    }

    public boolean getUseCaches() {
        return this.useCaches;
    }

    public void setIfModifiedSince(long ifmodifiedsince) {
        this.chequearNoConectado();
        this.ifModifiedSince = ifmodifiedsince;
    }

    public long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    /** El valor que toma `useCaches` en las conexiones nuevas del protocolo de esta. */
    public boolean getDefaultUseCaches() {
        return getDefaultUseCaches(this.protocoloDeLaUrl());
    }

    public void setDefaultUseCaches(boolean defaultusecaches) {
        setDefaultUseCaches(this.protocoloDeLaUrl(), defaultusecaches);
    }

    /**
     * El default de cache por protocolo. Ausente significa true, que es el default global.
     *
     * <p>El protocolo no distingue mayusculas: "HTTP" y "http" son el mismo.
     */
    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {
        synchronized (defaultUseCachesPorProtocolo) {
            defaultUseCachesPorProtocolo.put(protocol.toLowerCase(), Boolean.valueOf(defaultVal));
        }
    }

    public static boolean getDefaultUseCaches(String protocol) {
        synchronized (defaultUseCachesPorProtocolo) {
            Boolean v = defaultUseCachesPorProtocolo.get(protocol.toLowerCase());
            return v == null ? true : v.booleanValue();
        }
    }

    private String protocoloDeLaUrl() {
        return this.url == null ? "" : String.valueOf(this.url.getProtocol());
    }

    // ---- propiedades del pedido ----

    /** Fija la cabecera {@code key}, pisando lo que hubiera. */
    public void setRequestProperty(String key, String value) {
        this.chequearNoConectado();
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        String existente = this.claveExistente(key);
        if (existente != null) {
            this.requestProperties.remove(existente);
        }
        List<String> vals = new ArrayList<String>();
        vals.add(value);
        this.requestProperties.put(key, vals);
    }

    /** Agrega un valor mas a la cabecera {@code key}, sin pisar los que ya estaban. */
    public void addRequestProperty(String key, String value) {
        this.chequearNoConectado();
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        String existente = this.claveExistente(key);
        if (existente == null) {
            List<String> vals = new ArrayList<String>();
            vals.add(value);
            this.requestProperties.put(key, vals);
        } else {
            this.requestProperties.get(existente).add(value);
        }
    }

    /** El ULTIMO valor de {@code key}, o null. Que sea el ultimo y no el primero es lo del JDK. */
    public String getRequestProperty(String key) {
        this.chequearNoConectado();
        String existente = this.claveExistente(key);
        if (existente == null) {
            return null;
        }
        List<String> vals = this.requestProperties.get(existente);
        return vals.isEmpty() ? null : vals.get(vals.size() - 1);
    }

    /** Todas las cabeceras del pedido, de solo lectura. */
    public Map<String, List<String>> getRequestProperties() {
        this.chequearNoConectado();
        Map<String, List<String>> copia = new LinkedHashMap<String, List<String>>();
        Iterator<String> it = this.requestProperties.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            copia.put(k, Collections.unmodifiableList(
                    new ArrayList<String>(this.requestProperties.get(k))));
        }
        return Collections.unmodifiableMap(copia);
    }

    // La clave ya guardada que coincide con `key` sin distinguir caja, o null.
    private String claveExistente(String key) {
        if (key == null) {
            return null;
        }
        Iterator<String> it = this.requestProperties.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equalsIgnoreCase(key)) {
                return k;
            }
        }
        return null;
    }

    /**
     * No hace nada, y eso es lo que hace el JDK desde que se deprecio en 1.3.
     *
     * @deprecated los defaults por conexion se fijan con {@link #setRequestProperty}.
     */
    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }

    /**
     * Siempre null, como en el JDK.
     *
     * @deprecated ver {@link #setDefaultRequestProperty}.
     */
    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }

    // ---- lectura de la respuesta ----

    /**
     * Todas las cabeceras de la respuesta. En la clase base, el mapa vacio.
     *
     * <p>Es el metodo del que cuelgan casi todos los `getX` de abajo, y por eso una subclase que
     * lo sobreescriba bien hereda `getContentType`, `getDate`, `getLastModified` y el resto gratis.
     */
    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    /** El valor de la cabecera {@code name}, o null. */
    public String getHeaderField(String name) {
        return null;
    }

    /** El nombre de la cabecera numero {@code n}, o null si no hay tantas. */
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /** El valor de la cabecera numero {@code n}, o null si no hay tantas. */
    public String getHeaderField(int n) {
        return null;
    }

    /** La cabecera {@code name} como entero, o {@code Default} si falta o no es un numero. */
    public int getHeaderFieldInt(String name, int Default) {
        String value = this.getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return Default;
        }
    }

    /** Como {@link #getHeaderFieldInt}, en 64 bits. */
    public long getHeaderFieldLong(String name, long Default) {
        String value = this.getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return Default;
        }
    }

    /**
     * La cabecera {@code name} como instante, en milisegundos desde 1970.
     *
     * <p>Acepta los tres formatos que el protocolo permite (RFC 9110 5.6.7): el de preferencia
     * --`Sun, 06 Nov 1994 08:49:37 GMT`-- y los dos historicos que un servidor viejo todavia puede
     * mandar. Es computo puro sobre la cadena: no hay zona horaria que consultar, porque el
     * formato **obliga** a GMT.
     */
    public long getHeaderFieldDate(String name, long Default) {
        String value = this.getHeaderField(name);
        long t = parseHttpDate(value);
        return t == Long.MIN_VALUE ? Default : t;
    }

    /** El tamano del cuerpo, o -1 si no se sabe. Desborda a partir de 2 GiB: usar la version long. */
    public int getContentLength() {
        long l = this.getContentLengthLong();
        return l > Integer.MAX_VALUE ? -1 : (int) l;
    }

    /** El tamano del cuerpo en 64 bits, o -1 si no se sabe. */
    public long getContentLengthLong() {
        return this.getHeaderFieldLong("content-length", -1);
    }

    public String getContentType() {
        return this.getHeaderField("content-type");
    }

    public String getContentEncoding() {
        return this.getHeaderField("content-encoding");
    }

    /** Cuando vence el recurso, o 0 si no se sabe. */
    public long getExpiration() {
        return this.getHeaderFieldDate("expires", 0);
    }

    /** La fecha del mensaje, o 0 si no se sabe. */
    public long getDate() {
        return this.getHeaderFieldDate("date", 0);
    }

    /** Cuando se modifico el recurso por ultima vez, o 0 si no se sabe. */
    public long getLastModified() {
        return this.getHeaderFieldDate("last-modified", 0);
    }

    /**
     * El permiso que hace falta para hacer esta conexion.
     *
     * <p>La clase base devuelve `AllPermission`, igual que el JDK: no sabe a que va a conectarse,
     * asi que no puede pedir algo mas fino. Las subclases lo ajustan.
     */
    public java.security.Permission getPermission() throws IOException {
        return new java.security.AllPermission();
    }

    /**
     * El cuerpo como flujo de bytes.
     *
     * <p>En la clase base tira: un protocolo generico no sabe leer. Sobreescribir esto es la mitad
     * del trabajo de una subclase.
     *
     * @throws UnknownServiceException siempre, en la clase base
     */
    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }

    /**
     * Donde escribir el cuerpo del pedido.
     *
     * @throws UnknownServiceException siempre, en la clase base
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }

    /**
     * El cuerpo ya interpretado por el {@link ContentHandler} que corresponda a su tipo MIME.
     *
     * <p>Primero abre el flujo --y por eso en la clase base termina en la
     * `UnknownServiceException` de `getInputStream`, exactamente como en el JDK--; recien despues
     * busca manejador.
     *
     * <p>La busqueda de manejador es lo unico distinto: el JDK barre paquetes buscando una clase
     * por convencion de nombre; aca solo consulta la factoria que la aplicacion haya instalado con
     * {@link #setContentHandlerFactory}. Sin factoria no hay manejador, y eso se dice tirando, no
     * devolviendo algo inventado.
     */
    public Object getContent() throws IOException {
        this.getInputStream();
        return this.manejador().getContent(this);
    }

    /** Como {@link #getContent()}, pero devuelve null si el objeto no es de ninguna de {@code classes}. */
    public Object getContent(Class<?>[] classes) throws IOException {
        this.getInputStream();
        return this.manejador().getContent(this, classes);
    }

    private ContentHandler manejador() throws UnknownServiceException {
        String tipo = this.getContentType();
        if (tipo != null) {
            int puntoYComa = tipo.indexOf(';');
            if (puntoYComa != -1) {
                tipo = tipo.substring(0, puntoYComa).trim();
            }
        }
        if (tipo == null || tipo.length() == 0) {
            throw new UnknownServiceException("no content-type");
        }
        ContentHandlerFactory f = contentHandlerFactory;
        ContentHandler h = f == null ? null : f.createContentHandler(tipo);
        if (h == null) {
            throw new UnknownServiceException("no content handler for " + tipo);
        }
        return h;
    }

    /**
     * Instala la factoria de manejadores de contenido. Una sola vez por VM.
     *
     * @throws Error si ya se habia instalado una
     */
    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if (contentHandlerFactory != null) {
            throw new Error("factory already defined");
        }
        contentHandlerFactory = fac;
    }

    // ---- adivinar el tipo ----

    /** La tabla de extension a tipo MIME que usa {@link #guessContentTypeFromName}. */
    public static FileNameMap getFileNameMap() {
        FileNameMap m = fileNameMap;
        if (m == null) {
            synchronized (URLConnection.class) {
                if (fileNameMap == null) {
                    fileNameMap = new TablaDeExtensiones();
                }
                m = fileNameMap;
            }
        }
        return m;
    }

    public static void setFileNameMap(FileNameMap map) {
        fileNameMap = map;
    }

    /** El tipo MIME que sugiere la extension de {@code fname}, o null. */
    public static String guessContentTypeFromName(String fname) {
        return getFileNameMap().getContentTypeFor(fname);
    }

    /**
     * El tipo MIME que sugieren los primeros bytes de {@code is}, o null.
     *
     * <p>Mira y devuelve el flujo como estaba: usa `mark`/`reset`, y si el flujo no soporta marcas
     * devuelve null en vez de consumirlo. Consumir bytes de un flujo que despues alguien va a leer
     * seria un efecto colateral invisible.
     */
    public static String guessContentTypeFromStream(InputStream is) throws IOException {
        if (!is.markSupported()) {
            return null;
        }
        is.mark(16);
        int[] b = new int[16];
        int leidos = 0;
        while (leidos < 16) {
            int c = is.read();
            if (c == -1) {
                break;
            }
            b[leidos] = c;
            leidos = leidos + 1;
        }
        is.reset();
        return porNumeroMagico(b, leidos);
    }

    private static boolean empieza(int[] b, int n, int[] magico) {
        if (n < magico.length) {
            return false;
        }
        int i = 0;
        while (i < magico.length) {
            if (b[i] != magico[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static boolean empiezaTexto(int[] b, int n, String s) {
        return empieza(b, n, aBytes(s));
    }

    private static int[] aBytes(String s) {
        int[] out = new int[s.length()];
        int i = 0;
        while (i < s.length()) {
            out[i] = s.charAt(i) & 0xff;
            i = i + 1;
        }
        return out;
    }

    // La tabla de numeros magicos del JDK, transcripta **entera y sin agregados**.
    //
    // Lo de "sin agregados" es la parte que importa. La primera version de este metodo reconocia
    // ademas `%!` como PostScript y `.sd` como audio -- dos firmas reales, que cualquiera diria que
    // suman. Pero el JDK **no las reconoce**, asi que devolver un tipo ahi es dar una respuesta
    // distinta a la del JDK sobre los mismos bytes, y eso no es "mas completo": es incompatible.
    // La prueba de comportamiento las agarro.
    //
    // Por lo mismo, JPEG pide algo mas que `FF D8 FF`: el JDK mira el cuarto byte y solo acepta
    // E0, EE, o E1 seguido de "Exif\0". Un `FF D8 FF DB` --que es un JPEG perfectamente valido--
    // le da null, y aca tambien.
    private static String porNumeroMagico(int[] b, int n) {
        if (empieza(b, n, new int[] {0xCA, 0xFE, 0xBA, 0xBE})) {
            return "application/java-vm";
        }
        if (empieza(b, n, new int[] {0xAC, 0xED})) {
            return "application/x-java-serialized-object";
        }
        if (n >= 1 && b[0] == '<') {
            // Cualquier cosa que empiece con `<!` es HTML para el JDK -- no solo `<!DOCTYPE`. Por
            // eso este chequeo va antes que el de `<?xml`, y por eso no hay que enumerar las cajas
            // de "DOCTYPE".
            if (n >= 2 && b[1] == '!') {
                return "text/html";
            }
            if (empiezaTexto(b, n, "<html") || empiezaTexto(b, n, "<head")
                    || empiezaTexto(b, n, "<body") || empiezaTexto(b, n, "<HTML")
                    || empiezaTexto(b, n, "<HEAD") || empiezaTexto(b, n, "<BODY")) {
                return "text/html";
            }
            if (empiezaTexto(b, n, "<?xml ")) {
                return "application/xml";
            }
        }
        if (empiezaTexto(b, n, "! XPM2")) {
            return "image/x-pixmap";
        }
        if (empieza(b, n, new int[] {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (empieza(b, n, new int[] {0xFF, 0xD8, 0xFF}) && n >= 4) {
            if (b[3] == 0xE0 || b[3] == 0xEE) {
                return "image/jpeg";
            }
            if (b[3] == 0xE1 && n >= 11 && b[6] == 'E' && b[7] == 'x' && b[8] == 'i'
                    && b[9] == 'f' && b[10] == 0) {
                return "image/jpeg";
            }
        }
        if (empiezaTexto(b, n, "GIF8")) {
            return "image/gif";
        }
        if (empiezaTexto(b, n, "#def")) {
            return "image/x-bitmap";
        }
        if (empiezaTexto(b, n, ".snd")) {
            return "audio/basic";
        }
        if (empiezaTexto(b, n, "dns.")) {
            return "audio/basic";
        }
        if (empiezaTexto(b, n, "MThd")) {
            return "audio/midi";
        }
        if (empiezaTexto(b, n, "RIFF")) {
            return "audio/x-wav";
        }
        if (empieza(b, n, new int[] {0xF7, 0x02})) {
            return "application/x-dvi";
        }
        return null;
    }

    /**
     * {@code getClass().getName() + ":" + url}, como en el JDK.
     */
    @Override
    public String toString() {
        return this.getClass().getName() + ":" + this.url;
    }

    // ===========================================================================================
    // Fecha HTTP
    // ===========================================================================================

    // Devuelve `Long.MIN_VALUE` --y no una excepcion-- cuando no se entiende, porque el unico
    // llamador ya tiene un valor por defecto que dar y una excepcion ahi solo agregaria un
    // try/catch. Ningun instante real cae en `Long.MIN_VALUE`.
    static long parseHttpDate(String s) {
        if (s == null) {
            return Long.MIN_VALUE;
        }
        String v = s.trim();
        int coma = v.indexOf(',');
        if (coma != -1) {
            v = v.substring(coma + 1).trim();
        }
        // Quedan tres formas posibles:
        //   "06 Nov 1994 08:49:37 GMT"   (RFC 1123, la de preferencia)
        //   "06-Nov-94 08:49:37 GMT"     (RFC 850, historica; el ano de dos digitos es su problema)
        //   "Nov  6 08:49:37 1994"       (asctime, sin coma, por eso el recorte de arriba no la toco)
        v = v.replace('-', ' ');
        String[] p = partir(v);
        if (p.length >= 4 && esNumero(p[0])) {
            int dia = (int) parseEntero(p[0]);
            int mes = mesPorNombre(p[1]);
            long anio = parseEntero(p[2]);
            if (mes < 0 || anio == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            if (anio < 100) {
                // RFC 850: dos digitos. La convencion del JDK es la ventana 1970-2069.
                anio = anio < 70 ? anio + 2000 : anio + 1900;
            }
            return armar(anio, mes, dia, p[3]);
        }
        if (p.length >= 4 && mesPorNombre(p[0]) >= 0) {
            int mes = mesPorNombre(p[0]);
            int dia = (int) parseEntero(p[1]);
            long anio = parseEntero(p[3]);
            if (anio == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return armar(anio, mes, dia, p[2]);
        }
        return Long.MIN_VALUE;
    }

    private static long armar(long anio, int mes, int dia, String hora) {
        String[] hms = partirPor(hora, ':');
        if (hms.length < 2) {
            return Long.MIN_VALUE;
        }
        long h = parseEntero(hms[0]);
        long m = parseEntero(hms[1]);
        long sg = hms.length > 2 ? parseEntero(hms[2]) : 0;
        if (h == Long.MIN_VALUE || m == Long.MIN_VALUE || sg == Long.MIN_VALUE || dia < 1) {
            return Long.MIN_VALUE;
        }
        return (diasDesde1970(anio, mes, dia) * 86400L + h * 3600L + m * 60L + sg) * 1000L;
    }

    // Algoritmo de Howard Hinnant: cuenta los dias corridos de un calendario proleptico gregoriano
    // sin tablas ni bucles. Se usa este y no un `Calendar` porque una fecha HTTP siempre es GMT: no
    // hay zona ni horario de verano que consultar, y meter un `Calendar` traeria los dos.
    private static long diasDesde1970(long anio, int mes, int dia) {
        long y = anio;
        long m = mes + 1;
        y = y - (m <= 2 ? 1 : 0);
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + dia - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    private static final String[] MESES = {
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };

    private static int mesPorNombre(String s) {
        if (s == null || s.length() < 3) {
            return -1;
        }
        String tres = s.substring(0, 3).toLowerCase();
        int i = 0;
        while (i < MESES.length) {
            if (MESES[i].equals(tres)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    private static boolean esNumero(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static long parseEntero(String s) {
        return esNumero(s) ? Long.parseLong(s) : Long.MIN_VALUE;
    }

    private static String[] partir(String s) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        StringBuilder cur = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur = new StringBuilder();
                }
            } else {
                cur.append(c);
            }
            i = i + 1;
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    private static String[] partirPor(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start <= s.length()) {
            int i = s.indexOf(sep, start);
            if (i == -1) {
                out.add(s.substring(start));
                start = s.length() + 1;
            } else {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    // La tabla por defecto. El JDK lee `content-types.properties` de su propia instalacion; aca la
    // tabla esta escrita, con las mismas respuestas para las extensiones comunes (verificadas
    // contra el JDK real). Una extension que no esta da null, que es lo que corresponde: "no se".
    private static class TablaDeExtensiones implements FileNameMap {

        public String getContentTypeFor(String fileName) {
            if (fileName == null) {
                return null;
            }
            int punto = fileName.lastIndexOf('.');
            if (punto == -1 || punto == fileName.length() - 1) {
                return null;
            }
            String ext = fileName.substring(punto + 1).toLowerCase();
            if (ext.equals("html") || ext.equals("htm")) {
                return "text/html";
            }
            if (ext.equals("txt") || ext.equals("text")) {
                return "text/plain";
            }
            if (ext.equals("css")) {
                return "text/css";
            }
            if (ext.equals("js")) {
                return "text/javascript";
            }
            if (ext.equals("json")) {
                return "application/json";
            }
            if (ext.equals("xml")) {
                return "application/xml";
            }
            if (ext.equals("gif")) {
                return "image/gif";
            }
            if (ext.equals("png")) {
                return "image/png";
            }
            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("jpe")) {
                return "image/jpeg";
            }
            if (ext.equals("zip")) {
                return "application/zip";
            }
            if (ext.equals("gz")) {
                return "application/x-gzip";
            }
            if (ext.equals("pdf")) {
                return "application/pdf";
            }
            if (ext.equals("jar")) {
                return "application/java-archive";
            }
            if (ext.equals("class")) {
                return "application/java-vm";
            }
            if (ext.equals("java")) {
                return "text/plain";
            }
            return null;
        }
    }
}
