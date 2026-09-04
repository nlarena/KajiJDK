package java.net;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

/**
 * Una respuesta sacada de la cache que originalmente vino por una conexion segura.
 *
 * <h2>Por que hace falta un tipo aparte</h2>
 *
 * <p>Porque servir desde la cache pierde el canal. Una {@link CacheResponse} comun entrega los bytes
 * y nada mas, y entonces el codigo que decidia algo mirando el certificado del servidor —o la suite
 * acordada— <strong>no puede repetir esa decision</strong> cuando la respuesta viene de disco.
 *
 * <p>Esta clase es lo que hace que la cache no degrade la seguridad en silencio: guarda tambien lo
 * que se sabia del canal en el momento en que la respuesta se obtuvo, y lo devuelve. La informacion
 * es <em>historica</em> —describe la conexion original, no una actual— y esa es exactamente la
 * distincion que hay que tener presente al usarla.
 *
 * @since 1.5
 */
public abstract class SecureCacheResponse extends CacheResponse {

    /** Para las implementaciones de cache. */
    public SecureCacheResponse() {
    }

    /** La suite que se habia acordado. */
    public abstract String getCipherSuite();

    /**
     * Los certificados que se habian presentado, o {@code null} si no se presento ninguno.
     *
     * <p>La lista va del propio hacia la CA raiz, que es el orden del protocolo.
     */
    public abstract List<Certificate> getLocalCertificateChain();

    /**
     * Los certificados que habia presentado el servidor.
     *
     * @throws SSLPeerUnverifiedException si no se habia autenticado — puede pasar con una conexion
     *     perfectamente valida, porque cifrar y autenticar son cosas distintas
     */
    public abstract List<Certificate> getServerCertificateChain()
            throws SSLPeerUnverifiedException;

    /**
     * Quien era el servidor.
     *
     * @throws SSLPeerUnverifiedException si no se habia autenticado
     */
    public abstract Principal getPeerPrincipal() throws SSLPeerUnverifiedException;

    /** Quien se habia presentado como, o {@code null}. */
    public abstract Principal getLocalPrincipal();

    /**
     * La sesion original, si la cache la conservo.
     *
     * <p>Un {@link Optional} y con cuerpo: llego despues que el resto de la clase, y una cache vieja
     * no tiene por que haber guardado la sesion entera. Vacio significa "no la tengo", que es una
     * respuesta legitima y distinta de "no habia".
     *
     * @since 12
     */
    public Optional<SSLSession> getSSLSession() {
        return Optional.empty();
    }
}
