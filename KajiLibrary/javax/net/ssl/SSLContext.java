package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

/**
 * La fabrica de todo lo demas de este paquete.
 *
 * <h2>Que junta un contexto</h2>
 *
 * <p>Tres cosas que hasta ese momento estan sueltas: las credenciales propias ({@link KeyManager}),
 * la politica de confianza ({@link TrustManager}) y la fuente de aleatoriedad. Con las tres
 * configuradas, el contexto produce sockets, motores y fabricas que ya vienen con esa configuracion
 * adentro.
 *
 * <p>Es lo que permite tener dos politicas distintas en el mismo programa —una conexion a un
 * servicio interno con su CA propia, otra a Internet con las CA publicas— cosa que la configuracion
 * global por propiedades del sistema no permite.
 *
 * <h2>Sin proveedor de TLS instalado</h2>
 *
 * <p>{@link #getInstance} tira {@link NoSuchAlgorithmException}, que es la respuesta correcta y no
 * una carencia disfrazada: no hay ningun proveedor que ofrezca ese protocolo. Es el mismo criterio
 * que sigue {@code MessageDigest} en esta biblioteca. Quien quiera TLS registra un proveedor con
 * {@link Security#addProvider} y esto empieza a funcionar sin tocar una linea de aca.
 */
public class SSLContext {

    private static SSLContext laDefault;

    private final SSLContextSpi contextSpi;
    private final Provider provider;
    private final String protocol;

    /** Para los proveedores. */
    protected SSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
        this.contextSpi = contextSpi;
        this.provider = provider;
        this.protocol = protocol;
    }

    /**
     * El contexto por omision, ya inicializado.
     *
     * @throws NoSuchAlgorithmException si no hay proveedor que ofrezca el protocolo por omision
     */
    public static synchronized SSLContext getDefault() throws NoSuchAlgorithmException {
        if (laDefault == null) {
            laDefault = getInstance("Default");
        }
        return laDefault;
    }

    /**
     * Cambia el contexto por omision.
     *
     * @throws NullPointerException si es {@code null}
     */
    public static synchronized void setDefault(SSLContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        laDefault = context;
    }

    /**
     * El contexto de ese protocolo, del primer proveedor que lo ofrezca.
     *
     * @throws NoSuchAlgorithmException si ninguno lo ofrece
     */
    public static SSLContext getInstance(String protocol) throws NoSuchAlgorithmException {
        if (protocol == null) {
            throw new NullPointerException("protocol");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("SSLContext", protocol);
            if (s != null) {
                return armar(s, provs[i], protocol);
            }
        }
        throw new NoSuchAlgorithmException(protocol + " SSLContext not available");
    }

    /**
     * De un proveedor nombrado.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
     */
    public static SSLContext getInstance(String protocol, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(protocol, p);
    }

    /**
     * De un proveedor concreto.
     *
     * @throws NoSuchAlgorithmException si ese proveedor no ofrece el protocolo
     */
    public static SSLContext getInstance(String protocol, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (protocol == null) {
            throw new NullPointerException("protocol");
        }
        Provider.Service s = provider.getService("SSLContext", protocol);
        if (s == null) {
            throw new NoSuchAlgorithmException(protocol + " SSLContext not available");
        }
        return armar(s, provider, protocol);
    }

    private static SSLContext armar(Provider.Service s, Provider p, String protocol)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof SSLContextSpi)) {
                throw new NoSuchAlgorithmException(
                        "el proveedor no devolvio un SSLContextSpi para " + protocol);
            }
            return new SSLContext((SSLContextSpi) spi, p, protocol);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(protocol + " SSLContext not available", e);
        }
    }

    /** El protocolo de este contexto. */
    public final String getProtocol() {
        return this.protocol;
    }

    /** El proveedor que lo produjo. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Configura las tres fuentes.
     *
     * <p>Cualquiera de las tres puede ser {@code null}, y ahi se usa la por omision del proveedor.
     * Con {@code null} en los manejadores de confianza, eso significa las CA que el sistema ya
     * tenia — lo cual es lo que se quiere casi siempre, y conviene saber que es lo que pasa.
     */
    public final void init(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws KeyManagementException {
        this.contextSpi.engineInit(km, tm, random);
    }

    /** La fabrica de sockets cliente con esta configuracion. */
    public final SSLSocketFactory getSocketFactory() {
        return this.contextSpi.engineGetSocketFactory();
    }

    /** La fabrica de sockets servidor con esta configuracion. */
    public final SSLServerSocketFactory getServerSocketFactory() {
        return this.contextSpi.engineGetServerSocketFactory();
    }

    /** Un motor sin datos del par. */
    public final SSLEngine createSSLEngine() {
        return this.contextSpi.engineCreateSSLEngine();
    }

    /** Un motor con el par sugerido, lo que habilita reanudar sesion y mandar SNI. */
    public final SSLEngine createSSLEngine(String peerHost, int peerPort) {
        return this.contextSpi.engineCreateSSLEngine(peerHost, peerPort);
    }

    /** Las sesiones del lado servidor. */
    public final SSLSessionContext getServerSessionContext() {
        return this.contextSpi.engineGetServerSessionContext();
    }

    /** Las sesiones del lado cliente. */
    public final SSLSessionContext getClientSessionContext() {
        return this.contextSpi.engineGetClientSessionContext();
    }

    /** Los parametros por omision de este contexto. */
    public final SSLParameters getDefaultSSLParameters() {
        return this.contextSpi.engineGetDefaultSSLParameters();
    }

    /** Todo lo que este contexto soporta, sin importar que este habilitado. */
    public final SSLParameters getSupportedSSLParameters() {
        return this.contextSpi.engineGetSupportedSSLParameters();
    }
}
