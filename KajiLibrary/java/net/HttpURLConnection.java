package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;

// Una conexion HTTP: metodo, codigo de respuesta, redirecciones y streaming del cuerpo.
//
// ===========================================================================================
// ESTA CLASE ES ABSTRACTA, Y ESO ES TODA LA DIFERENCIA
// ===========================================================================================
//
// KajiJDK no tiene un cliente HTTP y no lo va a tener sin sockets. Pero `HttpURLConnection` **no es
// un cliente HTTP**: es la descripcion de uno. Sus dos metodos abstractos --`disconnect()` y
// `usingProxy()`-- son los unicos que necesitan saber si hay una conexion viva, y esta clase no los
// escribe: los declara.
//
// Todo lo demas es de esta clase y no necesita red:
//
//  - **El estado del pedido**: `method`, `instanceFollowRedirects`, los modos de streaming. Son
//    campos con sus validaciones, y las validaciones son reales: `setRequestMethod("BORRAR")` tira
//    `ProtocolException`, `setChunkedStreamingMode` despues de conectar tira `IllegalStateException`,
//    y fijar los dos modos de streaming a la vez tira, porque son excluyentes.
//  - **La lectura de la respuesta**: `getResponseCode()` y `getResponseMessage()` parsean la linea
//    de estado ("HTTP/1.1 404 Not Found") que la subclase haya puesto en la cabecera 0. Es parseo de
//    texto, es exacto, y esta entero.
//  - **Los cuarenta codigos de estado**, que son numeros acordados.
//  - **`getPermission()`**, que arma el `SocketPermission` del host y el puerto de la URL.
//
// Ninguno de esos miembros promete una conexion. La promesa esta concentrada en `connect()` --que
// hereda abstracto de `URLConnection`-- y en los dos abstractos de aca.
//
// ===========================================================================================
// QUIEN LA INSTANCIA
// ===========================================================================================
//
// **Nadie, en este arbol.** `URL.openConnection()` de una URL `http:` no llega hasta aca: tira
// antes, diciendo que no hay manejador para ese protocolo. Esta clase es el contrato que tendria que
// cumplir un cliente HTTP el dia que exista, y el tipo que las firmas pueden nombrar mientras tanto.
//
// Lo que no se hizo, y es la tentacion obvia: **no** hay una subclase concreta que devuelva 200 y un
// cuerpo vacio para que "algo ande". Eso seria una respuesta inventada presentada como una respuesta
// del servidor, que es la peor clase de mentira que se puede escribir en un cliente HTTP.
//
// Los sesenta y tres miembros estan.
public abstract class HttpURLConnection extends URLConnection {

    /** El metodo del pedido. */
    protected String method = "GET";

    /** El tamano de los trozos en modo chunked, o -1 si no esta en ese modo. */
    protected int chunkLength = -1;

    /**
     * El largo fijo del cuerpo, o -1.
     *
     * @deprecated Se desborda con cuerpos de mas de dos gigas; usar
     *             {@link #fixedContentLengthLong}.
     */
    @Deprecated
    protected int fixedContentLength = -1;

    /** El largo fijo del cuerpo, o -1 si no esta en ese modo. */
    protected long fixedContentLengthLong = -1;

    /** El codigo de estado, o -1 si todavia no se leyo. */
    protected int responseCode = -1;

    /** El texto que acompana al codigo de estado ("Not Found"), o null. */
    protected String responseMessage = null;

    /** Si esta conexion sigue las redirecciones sola. */
    protected boolean instanceFollowRedirects = followRedirects;

    private static boolean followRedirects = true;

    // Los metodos que el JDK acepta. La lista es cerrada a proposito: un metodo cualquiera se
    // rechaza, porque `URLConnection` no sabria como armar el pedido.
    private static final String[] METODOS = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"};

    /** Construye la conexion sin conectarla. */
    protected HttpURLConnection(URL u) {
        super(u);
    }

    // ---- redirecciones --------------------------------------------------------------------------

    /** Si las conexiones **nuevas** siguen redirecciones. */
    public static void setFollowRedirects(boolean set) {
        followRedirects = set;
    }

    /** Si las conexiones nuevas siguen redirecciones. */
    public static boolean getFollowRedirects() {
        return followRedirects;
    }

    /**
     * Si **esta** conexion sigue redirecciones.
     *
     * <p>Que haya una bandera por instancia ademas de la de toda la VM es lo que permite decir "esta
     * no", que es lo que hace falta para inspeccionar un `301` en vez de seguirlo.
     *
     * @throws IllegalStateException si ya se conecto
     */
    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.instanceFollowRedirects = followRedirects;
    }

    /** Si esta conexion sigue redirecciones. */
    public boolean getInstanceFollowRedirects() {
        return this.instanceFollowRedirects;
    }

    // ---- el pedido ------------------------------------------------------------------------------

    /**
     * El metodo del pedido: GET, POST, HEAD, OPTIONS, PUT, DELETE o TRACE.
     *
     * @throws ProtocolException     si el metodo no es uno de esos, o si ya se conecto
     * @throws IllegalStateException nunca -- el JDK usa `ProtocolException` tambien para el caso de
     *                               "ya conectado", y se respeta
     */
    public void setRequestMethod(String method) throws ProtocolException {
        if (this.connected) {
            throw new ProtocolException("Can't reset method: already connected");
        }
        for (int i = 0; i < METODOS.length; i++) {
            if (METODOS[i].equals(method)) {
                this.method = method;
                return;
            }
        }
        throw new ProtocolException("Invalid HTTP method: " + method);
    }

    /** El metodo del pedido. */
    public String getRequestMethod() {
        return this.method;
    }

    /**
     * Manda el cuerpo con un largo conocido de antemano, sin juntarlo todo en memoria.
     *
     * <p>Sirve para subir un archivo grande: sin esto, `URLConnection` tiene que acumular el cuerpo
     * entero para poder poner el `Content-Length`.
     *
     * @throws IllegalStateException    si ya se conecto o ya se fijo el modo chunked
     * @throws IllegalArgumentException si el largo es negativo
     * @deprecated Se desborda con cuerpos de mas de dos gigas; usar la sobrecarga con `long`.
     */
    @Deprecated
    public void setFixedLengthStreamingMode(int contentLength) {
        chequearModoDeStreaming();
        if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLength = contentLength;
    }

    /**
     * Manda el cuerpo con un largo conocido de antemano.
     *
     * @throws IllegalStateException    si ya se conecto o ya se fijo el modo chunked
     * @throws IllegalArgumentException si el largo es negativo
     */
    public void setFixedLengthStreamingMode(long contentLength) {
        chequearModoDeStreaming();
        if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLengthLong = contentLength;
    }

    /**
     * Manda el cuerpo en trozos, sin saber de antemano cuanto mide.
     *
     * <p>Es el otro modo de no acumular en memoria, y el que sirve cuando el largo **no se puede**
     * saber -- una respuesta generada al vuelo, por ejemplo.
     *
     * @param chunklen el tamano de trozo sugerido; {@code <= 0} deja elegir a la implementacion
     * @throws IllegalStateException si ya se conecto o ya se fijo un largo fijo
     */
    public void setChunkedStreamingMode(int chunklen) {
        chequearModoDeStreaming();
        if (chunklen <= 0) {
            this.chunkLength = 4096;
        } else {
            this.chunkLength = chunklen;
        }
    }

    // Los dos modos de streaming son excluyentes: uno dice cuanto mide el cuerpo y el otro dice que
    // no se sabe. Tenerlos juntos no significa nada, y por eso se rechaza en vez de elegir uno.
    private void chequearModoDeStreaming() {
        if (this.connected) {
            throw new IllegalStateException("Can't set streaming mode: already connected");
        }
        if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        }
        if (this.fixedContentLength != -1 || this.fixedContentLengthLong != -1) {
            throw new IllegalStateException("Fixed length streaming mode set");
        }
    }

    /**
     * Instala el autenticador de esta conexion.
     *
     * <p>La base tira `UnsupportedOperationException`, igual que el JDK: no toda implementacion sabe
     * autenticar por conexion --el JDK tiene un autenticador global desde antes que este metodo
     * existiera-- y la que sepa lo pisa. Aceptarlo en silencio seria peor: el que llama creeria que
     * sus credenciales se van a usar.
     *
     * @throws UnsupportedOperationException siempre, en la implementacion base
     * @throws NullPointerException          si {@code auth} es null
     */
    public void setAuthenticator(Authenticator auth) {
        throw new UnsupportedOperationException(
                "Supplying an authenticator is not supported by " + this.getClass());
    }

    // ---- la respuesta ---------------------------------------------------------------------------

    /**
     * El nombre de la cabecera numero {@code n}, o null.
     *
     * <p>Devuelve null para {@code n == 0} aun cuando haya cabecera 0, porque la cabecera 0 es la
     * **linea de estado** y no tiene nombre. Esa convencion es la que hace que `getHeaderField(0)`
     * devuelva "HTTP/1.1 200 OK".
     */
    @Override
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /** El valor de la cabecera numero {@code n}, o null. La base no tiene cabeceras. */
    @Override
    public String getHeaderField(int n) {
        return null;
    }

    /**
     * El codigo de estado: 200, 404, 500.
     *
     * <p>Sale de parsear la linea de estado que la subclase dejo en la cabecera 0. Devuelve -1 si no
     * hay linea de estado o si no tiene la forma esperada -- no se inventa un codigo.
     *
     * @throws IOException si falla la conexion mientras se lee la respuesta
     */
    public int getResponseCode() throws IOException {
        if (this.responseCode != -1) {
            return this.responseCode;
        }

        // Primero se fuerza la conexion, porque la linea de estado no existe hasta que el otro lado
        // contesto. La excepcion se GUARDA en vez de propagarse: si igual aparecio una linea de
        // estado, el pedido se completo y el fallo era de otra cosa --leer el cuerpo, tipicamente--
        // y tapar el codigo de respuesta con esa excepcion perderia justo lo que se estaba pidiendo.
        Exception falla = null;
        try {
            getInputStream();
        } catch (Exception e) {
            falla = e;
        }

        String lineaDeEstado = getHeaderField(0);
        if (lineaDeEstado == null) {
            // Sin linea de estado no hubo respuesta, y ahi la excepcion guardada SI es la
            // explicacion; devolver -1 y tragarsela dejaria al que llamo sin saber que paso.
            if (falla != null) {
                if (falla instanceof RuntimeException) {
                    throw (RuntimeException) falla;
                }
                throw (IOException) falla;
            }
            return -1;
        }

        // "HTTP-Version SP Status-Code SP Reason-Phrase", del RFC 2616. La frase es opcional: hay
        // servidores que la omiten, y el JDK los acepta a proposito.
        if (lineaDeEstado.startsWith("HTTP/1.")) {
            int posCodigo = lineaDeEstado.indexOf(' ');
            if (posCodigo > 0) {
                int posFrase = lineaDeEstado.indexOf(' ', posCodigo + 1);
                if (posFrase > 0 && posFrase < lineaDeEstado.length()) {
                    // Sin recortar: la frase es lo que vino, espacios incluidos.
                    this.responseMessage = lineaDeEstado.substring(posFrase + 1);
                }
                if (posFrase < 0) {
                    posFrase = lineaDeEstado.length();
                }
                try {
                    this.responseCode =
                            Integer.parseInt(lineaDeEstado.substring(posCodigo + 1, posFrase));
                    return this.responseCode;
                } catch (NumberFormatException noEsUnCodigo) {
                    // Cae al -1 de abajo: no se inventa un codigo.
                }
            }
        }
        return -1;
    }

    /**
     * El texto que acompana al codigo ("Not Found"), o null si no vino ninguno.
     *
     * @throws IOException si falla la conexion mientras se lee la respuesta
     */
    public String getResponseMessage() throws IOException {
        getResponseCode();
        return this.responseMessage;
    }

    /**
     * Esa cabecera leida como fecha.
     *
     * <p>Se pisa la version de `URLConnection` para agregarle "GMT" a una fecha que no lo traiga,
     * que es lo que hace el JDK: hay servidores que lo omiten, y todas las gramaticas de fecha de
     * HTTP son en GMT de todos modos.
     */
    @Override
    public long getHeaderFieldDate(String name, long Default) {
        String texto = getHeaderField(name);
        if (texto == null) {
            return Default;
        }
        if (texto.indexOf("GMT") == -1) {
            texto = texto + " GMT";
        }
        long parseada = HttpCookie.parseCookieDate(texto);
        if (parseada == Long.MIN_VALUE) {
            return Default;
        }
        return parseada;
    }

    /**
     * El flujo con el cuerpo de un error, o null.
     *
     * <p>Existe porque un `404` **tambien trae cuerpo**, y `getInputStream()` tira para cualquier
     * codigo de error: sin este metodo, la pagina que explica el error seria inalcanzable. Devuelve
     * null cuando no hay error, no hay cuerpo, o la conexion no llego a establecerse.
     *
     * <p>La base devuelve null, como en el JDK.
     */
    public InputStream getErrorStream() {
        return null;
    }

    /**
     * El permiso que hace falta para hacer este pedido.
     *
     * <p>Un `SocketPermission` de conexion al host y puerto de la URL, que es lo que el JDK devuelve.
     * Se puede armar entero aca porque construir un permiso es texto, no red.
     *
     * @throws IOException si no se pudo determinar
     */
    @Override
    public Permission getPermission() throws IOException {
        int puerto = this.url.getPort();
        if (puerto < 0) {
            puerto = 80;
        }
        String host = this.url.getHost() + ":" + puerto;
        return new SocketPermission(host, "connect");
    }

    /**
     * Suelta la conexion; los pedidos que siguen abren una nueva.
     *
     * <p>Abstracto: es uno de los dos metodos que necesitan saber si hay algo vivo del otro lado.
     */
    public abstract void disconnect();

    /**
     * Si este pedido sale por un proxy.
     *
     * <p>Abstracto: solo la implementacion sabe por donde salio.
     */
    public abstract boolean usingProxy();

    // ---- codigos de estado ----------------------------------------------------------------------

    /** 200: salio bien. */
    public static final int HTTP_OK = 200;

    /** 201: se creo el recurso. */
    public static final int HTTP_CREATED = 201;

    /** 202: se acepto, pero todavia no se hizo. */
    public static final int HTTP_ACCEPTED = 202;

    /** 203: la informacion viene de una copia, no del origen. */
    public static final int HTTP_NOT_AUTHORITATIVE = 203;

    /** 204: salio bien y no hay cuerpo. */
    public static final int HTTP_NO_CONTENT = 204;

    /** 205: salio bien; el cliente deberia limpiar el formulario. */
    public static final int HTTP_RESET = 205;

    /** 206: viene solo el pedazo que se pidio. */
    public static final int HTTP_PARTIAL = 206;

    /** 300: hay varias respuestas posibles. */
    public static final int HTTP_MULT_CHOICE = 300;

    /** 301: se mudo, y para siempre. */
    public static final int HTTP_MOVED_PERM = 301;

    /** 302: se mudo, por ahora. */
    public static final int HTTP_MOVED_TEMP = 302;

    /** 303: mira en otro lado, con un GET. */
    public static final int HTTP_SEE_OTHER = 303;

    /** 304: no cambio desde la fecha que mandaste. */
    public static final int HTTP_NOT_MODIFIED = 304;

    /** 305: hay que pasar por un proxy. */
    public static final int HTTP_USE_PROXY = 305;

    /** 400: el pedido esta mal formado. */
    public static final int HTTP_BAD_REQUEST = 400;

    /** 401: hace falta autenticarse. */
    public static final int HTTP_UNAUTHORIZED = 401;

    /** 402: reservado para pagos; casi nadie lo usa. */
    public static final int HTTP_PAYMENT_REQUIRED = 402;

    /** 403: te identificaste y aun asi no podes. */
    public static final int HTTP_FORBIDDEN = 403;

    /** 404: no existe. */
    public static final int HTTP_NOT_FOUND = 404;

    /** 405: ese metodo no vale para este recurso. */
    public static final int HTTP_BAD_METHOD = 405;

    /** 406: no se puede dar en ninguno de los formatos que aceptas. */
    public static final int HTTP_NOT_ACCEPTABLE = 406;

    /** 407: hay que autenticarse ante el proxy. */
    public static final int HTTP_PROXY_AUTH = 407;

    /** 408: el cliente tardo demasiado en mandar el pedido. */
    public static final int HTTP_CLIENT_TIMEOUT = 408;

    /** 409: choca con el estado actual del recurso. */
    public static final int HTTP_CONFLICT = 409;

    /** 410: existia y ya no, a proposito. */
    public static final int HTTP_GONE = 410;

    /** 411: falta el `Content-Length`. */
    public static final int HTTP_LENGTH_REQUIRED = 411;

    /** 412: no se cumplio una condicion del pedido. */
    public static final int HTTP_PRECON_FAILED = 412;

    /** 413: el cuerpo es demasiado grande. */
    public static final int HTTP_ENTITY_TOO_LARGE = 413;

    /** 414: la URL es demasiado larga. */
    public static final int HTTP_REQ_TOO_LONG = 414;

    /** 415: el tipo del cuerpo no se acepta. */
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /**
     * 500: error del servidor.
     *
     * @deprecated Estaba mal nombrado desde el principio; el nombre bueno es
     *             {@link #HTTP_INTERNAL_ERROR}. Se conserva porque hay codigo que lo usa.
     */
    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;

    /** 500: el servidor se rompio. */
    public static final int HTTP_INTERNAL_ERROR = 500;

    /** 501: el servidor no sabe hacer eso. */
    public static final int HTTP_NOT_IMPLEMENTED = 501;

    /** 502: el de mas atras contesto cualquier cosa. */
    public static final int HTTP_BAD_GATEWAY = 502;

    /** 503: no esta disponible ahora. */
    public static final int HTTP_UNAVAILABLE = 503;

    /** 504: el de mas atras no contesto a tiempo. */
    public static final int HTTP_GATEWAY_TIMEOUT = 504;

    /** 505: esa version de HTTP no se soporta. */
    public static final int HTTP_VERSION = 505;
}
