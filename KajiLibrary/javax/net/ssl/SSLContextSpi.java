package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.SecureRandom;

/**
 * Lo que un proveedor implementa para que exista un {@link SSLContext}.
 *
 * <p>Los dos ultimos metodos tienen cuerpo y los demas no, y la diferencia dice algo: un proveedor
 * viejo no conocia {@link SSLParameters}, asi que volverlos abstractos lo habria roto. Llegan
 * tirando {@link UnsupportedOperationException}, que es honesto — el proveedor no sabe contestar esa
 * pregunta— y no un valor inventado.
 */
public abstract class SSLContextSpi {

    public SSLContextSpi() {
    }

    /** Inicializa con las fuentes de credenciales, de confianza y de aleatoriedad. */
    protected abstract void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr)
            throws KeyManagementException;

    /** La fabrica de sockets cliente de este contexto. */
    protected abstract SSLSocketFactory engineGetSocketFactory();

    /** La fabrica de sockets servidor. */
    protected abstract SSLServerSocketFactory engineGetServerSocketFactory();

    /** Un motor sin datos del par. */
    protected abstract SSLEngine engineCreateSSLEngine();

    /** Un motor con el par sugerido, que habilita reanudar sesiones y mandar SNI. */
    protected abstract SSLEngine engineCreateSSLEngine(String host, int port);

    /** El contexto de sesiones del lado servidor. */
    protected abstract SSLSessionContext engineGetServerSessionContext();

    /** El contexto de sesiones del lado cliente. */
    protected abstract SSLSessionContext engineGetClientSessionContext();

    /**
     * Los parametros por omision.
     *
     * @throws UnsupportedOperationException si el proveedor no los sabe informar
     */
    protected SSLParameters engineGetDefaultSSLParameters() {
        throw new UnsupportedOperationException(
                "este proveedor no informa sus parametros por omision");
    }

    /**
     * Los parametros que soporta.
     *
     * @throws UnsupportedOperationException si el proveedor no los sabe informar
     */
    protected SSLParameters engineGetSupportedSSLParameters() {
        throw new UnsupportedOperationException(
                "este proveedor no informa los parametros que soporta");
    }
}
