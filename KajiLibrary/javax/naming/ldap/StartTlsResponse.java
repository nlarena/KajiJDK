package javax.naming.ldap;

import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * La respuesta a un {@link StartTlsRequest}, que ademas <strong>hace</strong> la negociacion.
 *
 * <h2>Por que una respuesta tiene metodos que actuan</h2>
 *
 * <p>Es la anomalia de esta clase, y tiene motivo: el servidor contesta que acepta cambiar a TLS, y
 * el handshake tiene que pasar <em>sobre la misma conexion</em>. El unico objeto que tiene esa
 * conexion a mano es el que el proveedor construyo para la respuesta.
 *
 * <p>De ahi el orden obligado: configurar —{@link #setEnabledCipherSuites},
 * {@link #setHostnameVerifier}— y recien despues {@link #negotiate}. Configurar despues no hace
 * nada.
 *
 * <h2>El verificador de nombre, que es el punto delicado</h2>
 *
 * <p>Tras el handshake hay que comprobar que el certificado corresponde al servidor al que se creia
 * uno conectado. La implementacion por omision lo hace; poner un {@link HostnameVerifier} permisivo
 * lo desactiva, y con eso se pierde la proteccion contra un intermediario — que es justamente lo que
 * StartTLS venia a resolver.
 */
public abstract class StartTlsResponse implements ExtendedResponse {

    private static final long serialVersionUID = 8372842182579276418L;

    /** El OID de la operacion. */
    public static final String OID = "1.3.6.1.4.1.1466.20037";

    /** Para las implementaciones del proveedor. */
    protected StartTlsResponse() {
    }

    public String getID() {
        return OID;
    }

    /** {@code null}: esta respuesta no lleva datos. */
    public byte[] getEncodedValue() {
        return null;
    }

    /**
     * Restringe las suites a usar. Hay que llamarlo <strong>antes</strong> de {@link #negotiate}.
     */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** Cambia como se verifica el nombre; ver la nota de la clase antes de usarlo. */
    public abstract void setHostnameVerifier(HostnameVerifier verifier);

    /**
     * Hace el handshake con la fabrica de sockets por omision.
     *
     * @throws IOException si el handshake falla, o si el nombre no verifica
     */
    public abstract SSLSession negotiate() throws IOException;

    /** Igual, con esa fabrica — asi se usa un contexto TLS propio. */
    public abstract SSLSession negotiate(SSLSocketFactory factory) throws IOException;

    /**
     * Cierra la capa TLS y vuelve a la conexion en claro.
     *
     * <p>La conexion LDAP <strong>sigue</strong>: esto no la cierra. Es lo que permite bajar el
     * cifrado despues de una operacion sensible sin reconectar — aunque en la practica casi nadie
     * quiera eso.
     */
    public abstract void close() throws IOException;
}
