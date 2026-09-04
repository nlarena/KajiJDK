package javax.net.ssl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * Una {@link HttpURLConnection} sobre TLS, con lo que hace falta para inspeccionar el canal.
 *
 * <h2>Lo que agrega, y por que</h2>
 *
 * <p>{@code HttpURLConnection} entrega el contenido y no dice nada de como viajo. Con HTTPS eso no
 * alcanza: hay decisiones que dependen de con quien se hablo realmente —que certificado presento,
 * que suite se acordo— y sin estos metodos habria que confiar a ciegas en que la biblioteca hizo
 * bien la verificacion.
 *
 * <h2>Los dos niveles de configuracion</h2>
 *
 * <p>Cada opcion viene por duplicado: una estatica y una de instancia. La estatica cambia el valor
 * por omision de <em>toda conexion futura</em>; la de instancia solo esta. Estan las dos porque el
 * caso comun —confiar en una CA propia para todo el programa— no deberia obligar a tocar cada
 * conexion, y el caso raro —una sola conexion distinta— no deberia obligar a cambiarle la politica a
 * todas.
 *
 * <p>Cuidado con la estatica: es estado global, y un {@link HostnameVerifier} permisivo puesto ahi
 * desactiva la verificacion de identidad en todo el programa, incluido el codigo que no escribio
 * quien lo puso.
 */
public abstract class HttpsURLConnection extends HttpURLConnection {

    /**
     * El verificador por omision: rechaza siempre.
     *
     * <p>Y es lo correcto, no una limitacion: este verificador solo se consulta cuando la
     * verificacion estandar de identidad <strong>ya fallo</strong>, asi que devolver {@code true}
     * seria aceptar un certificado que no corresponde al destino.
     *
     * <p>Es una clase con nombre y no una anonima —que es como lo escribe el JDK— por el finding
     * #499: nuestro generador no soporta una clase anonima en un inicializador de campo. Adentro de
     * un metodo si, y con nombre tambien; el cruce de los dos es lo que falla.
     */
    private static final class RechazaTodo implements HostnameVerifier {

        public boolean verify(String hostname, SSLSession session) {
            return false;
        }
    }

    private static HostnameVerifier defaultHostnameVerifier = new RechazaTodo();

    private static SSLSocketFactory defaultSSLSocketFactory;

    /** El verificador de esta conexion. */
    protected HostnameVerifier hostnameVerifier = defaultHostnameVerifier;

    private SSLSocketFactory sslSocketFactory = getDefaultSSLSocketFactory();

    /** Para las subclases. */
    protected HttpsURLConnection(URL url) {
        super(url);
    }

    /**
     * La suite acordada.
     *
     * @throws IllegalStateException si la conexion todavia no se establecio — no hay suite antes del
     *     handshake, y devolver {@code null} dejaria pasar la pregunta mal hecha
     */
    public abstract String getCipherSuite();

    /** Los certificados que se presentaron, o {@code null}. */
    public abstract Certificate[] getLocalCertificates();

    /**
     * Los certificados del servidor.
     *
     * @throws SSLPeerUnverifiedException si no se autentico
     */
    public abstract Certificate[] getServerCertificates() throws SSLPeerUnverifiedException;

    /**
     * Quien es el servidor.
     *
     * <p>Por omision sale del primer certificado de {@link #getServerCertificates}, que es lo que
     * corresponde con X.509. Una subclase que use otra autenticacion lo sobrescribe.
     *
     * @throws SSLPeerUnverifiedException si no se autentico
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        Certificate[] certs = getServerCertificates();
        if (certs.length == 0 || !(certs[0] instanceof java.security.cert.X509Certificate)) {
            throw new SSLPeerUnverifiedException("no hay certificado X.509 del par");
        }
        return ((java.security.cert.X509Certificate) certs[0]).getSubjectX500Principal();
    }

    /** Quien nos presentamos como, o {@code null}. */
    public Principal getLocalPrincipal() {
        Certificate[] certs = getLocalCertificates();
        if (certs == null || certs.length == 0
                || !(certs[0] instanceof java.security.cert.X509Certificate)) {
            return null;
        }
        return ((java.security.cert.X509Certificate) certs[0]).getSubjectX500Principal();
    }

    /**
     * Cambia el verificador por omision de todas las conexiones futuras.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public static void setDefaultHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException("el verificador no puede ser null");
        }
        defaultHostnameVerifier = v;
    }

    /** El verificador por omision. */
    public static HostnameVerifier getDefaultHostnameVerifier() {
        return defaultHostnameVerifier;
    }

    /**
     * Cambia el verificador de esta conexion.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException("el verificador no puede ser null");
        }
        this.hostnameVerifier = v;
    }

    /** El verificador de esta conexion. */
    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    /**
     * Cambia la fabrica de sockets por omision.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public static void setDefaultSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException("la fabrica no puede ser null");
        }
        defaultSSLSocketFactory = sf;
    }

    /** La fabrica por omision; la de {@link SSLSocketFactory#getDefault} si no se cambio. */
    public static synchronized SSLSocketFactory getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return defaultSSLSocketFactory;
    }

    /**
     * Cambia la fabrica de esta conexion. Es como se usa un {@link SSLContext} propio en una sola.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException("la fabrica no puede ser null");
        }
        this.sslSocketFactory = sf;
    }

    /** La fabrica de esta conexion. */
    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * La sesion, si hay.
     *
     * <p>Un {@link Optional} y no {@code null} porque llego despues, y porque la respuesta legitima
     * es "todavia no" — llega antes de que la conexion se establezca.
     */
    public Optional<SSLSession> getSSLSession() {
        return Optional.empty();
    }
}
