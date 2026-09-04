package javax.net.ssl;

import java.security.Principal;
import java.security.cert.Certificate;

/**
 * Lo que dos puntas negociaron una vez y pueden reusar muchas.
 *
 * <h2>Por que una sesion no es una conexion</h2>
 *
 * <p>Es la distincion central de este tipo. El handshake completo es caro —criptografia asimetrica,
 * varios viajes de ida y vuelta— y una aplicacion abre y cierra conexiones todo el tiempo. La sesion
 * guarda lo acordado (la suite, el secreto maestro, los certificados) para que una conexion nueva
 * pueda <em>reanudarla</em> con un handshake abreviado. Muchas conexiones, una sesion.
 *
 * <p>De ahi que {@link #invalidate} no cierre nada: solo prohibe que futuras conexiones la reanuden.
 *
 * <h2>El almacen de valores</h2>
 *
 * <p>{@link #putValue} y compania dejan colgarle datos de la aplicacion, y sirve justamente porque
 * la sesion sobrevive a la conexion: es donde poner algo que vale para todas las conexiones con ese
 * par. Un valor que implemente {@link SSLSessionBindingListener} se entera cuando entra y sale.
 */
public interface SSLSession {

    /** El identificador que le puso el servidor. */
    byte[] getId();

    /** El contexto que la administra, o {@code null} si no esta en ninguno. */
    SSLSessionContext getSessionContext();

    /** Cuando se creo, en milisegundos desde la epoca. */
    long getCreationTime();

    /** Cuando se uso por ultima vez. Es lo que mira el contexto para vencerla. */
    long getLastAccessedTime();

    /**
     * Prohibe reanudarla.
     *
     * <p>No cierra las conexiones que ya la estan usando: esas siguen. Lo que impide es que una
     * conexion nueva se ahorre el handshake completo.
     */
    void invalidate();

    /** Si todavia se puede reanudar. */
    boolean isValid();

    /** Guarda un valor de la aplicacion. */
    void putValue(String name, Object value);

    /** El valor guardado con ese nombre, o {@code null}. */
    Object getValue(String name);

    /** Saca un valor. */
    void removeValue(String name);

    /** Los nombres de los valores guardados. */
    String[] getValueNames();

    /**
     * Los certificados que presento el par.
     *
     * @throws SSLPeerUnverifiedException si el par no se autentico — lo que puede pasar con una
     *     sesion perfectamente valida, porque cifrar y autenticar son cosas distintas
     */
    Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

    /** Los certificados que se presentaron, o {@code null} si no se presento ninguno. */
    Certificate[] getLocalCertificates();

    /**
     * Los certificados del par, en el tipo viejo de {@code javax.security.cert}.
     *
     * @deprecated ese paquete quedo obsoleto; usar {@link #getPeerCertificates}
     */
    @Deprecated(since = "9")
    default javax.security.cert.X509Certificate[] getPeerCertificateChain()
            throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException(
                "esta sesion no soporta el tipo obsoleto javax.security.cert");
    }

    /**
     * Quien es el par.
     *
     * @throws SSLPeerUnverifiedException si no se autentico
     */
    Principal getPeerPrincipal() throws SSLPeerUnverifiedException;

    /** Quien nos presentamos como, o {@code null}. */
    Principal getLocalPrincipal();

    /** La suite de cifrado acordada. */
    String getCipherSuite();

    /** La version de protocolo acordada. */
    String getProtocol();

    /** El nombre del par tal como se pidio, sin resolver ni verificar. */
    String getPeerHost();

    /** El puerto del par. */
    int getPeerPort();

    /**
     * El buffer mas grande que hace falta para un registro de red.
     *
     * <p>Es mayor que {@link #getApplicationBufferSize}: un registro TLS lleva encabezado, relleno y
     * MAC ademas de los datos. Quien usa un {@link SSLEngine} dimensiona con estos dos numeros y no
     * adivinando, o se come un {@code BUFFER_OVERFLOW} en el peor momento.
     */
    int getPacketBufferSize();

    /** Los datos mas grandes que puede entregar de una. */
    int getApplicationBufferSize();
}
