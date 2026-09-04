package javax.rmi.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * La fabrica que hace que el lado servidor de una llamada RMI escuche por SSL.
 *
 * <p>Es la contraparte de {@link SslRMIClientSocketFactory}, y a diferencia de aquella **si** tiene
 * estado: las suites, los protocolos y la exigencia de certificado de cliente son decisiones del
 * servidor, se configuran aca y no viajan a ninguna parte.
 *
 * <h2>Por que valida en el constructor</h2>
 *
 * <p>El constructor fabrica un {@link SSLSocket} de prueba solo para comprobar que las suites y los
 * protocolos que le pasaron existen. Es a proposito: si no lo hiciera, un nombre mal escrito no se
 * notaria hasta la primera conexion entrante --y ahi el error aparece en el hilo del `accept`, sin
 * relacion visible con la linea que lo configuro mal.
 *
 * <h2>Sobre `equals`</h2>
 *
 * <p>Dos fabricas son iguales si tienen la **misma clase** y la misma configuracion. La clase
 * cuenta porque una subclase puede agregar estado que esta clase no sabe comparar, y dos fabricas
 * "iguales" hacen que RMI comparta un socket de escucha entre objetos exportados.
 */
public class SslRMIServerSocketFactory implements RMIServerSocketFactory {

    private static SSLSocketFactory defaultSSLSocketFactory = null;

    private final String[] enabledCipherSuites;
    private final String[] enabledProtocols;
    private final boolean needClientAuth;
    private List<String> enabledCipherSuitesList;
    private List<String> enabledProtocolsList;
    private SSLContext context;

    /**
     * Una fabrica con la configuracion SSL de siempre y sin exigir certificado de cliente.
     */
    public SslRMIServerSocketFactory() {
        this(null, null, null, false);
    }

    /**
     * Una fabrica con las suites y los protocolos dados.
     *
     * @param enabledCipherSuites las suites a habilitar, o `null` para las de siempre
     * @param enabledProtocols los protocolos a habilitar, o `null` para los de siempre
     * @param needClientAuth si se le exige certificado al cliente
     * @throws IllegalArgumentException si alguna suite o protocolo no esta soportado, o si no se
     *     pudo averiguar
     */
    public SslRMIServerSocketFactory(String[] enabledCipherSuites, String[] enabledProtocols,
            boolean needClientAuth) throws IllegalArgumentException {
        this(null, enabledCipherSuites, enabledProtocols, needClientAuth);
    }

    /**
     * Una fabrica que saca sus sockets de ese contexto.
     *
     * @param context el contexto SSL, o `null` para el de siempre
     * @param enabledCipherSuites las suites a habilitar, o `null` para las de siempre
     * @param enabledProtocols los protocolos a habilitar, o `null` para los de siempre
     * @param needClientAuth si se le exige certificado al cliente
     * @throws IllegalArgumentException si alguna suite o protocolo no esta soportado, o si no se
     *     pudo averiguar
     */
    public SslRMIServerSocketFactory(SSLContext context, String[] enabledCipherSuites,
            String[] enabledProtocols, boolean needClientAuth) throws IllegalArgumentException {
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites == null ? null : enabledCipherSuites.clone();
        this.enabledProtocols = enabledProtocols == null ? null : enabledProtocols.clone();
        this.needClientAuth = needClientAuth;

        if (this.enabledCipherSuites == null && this.enabledProtocols == null) {
            return;
        }

        // Las listas son para `equals`: comparar arreglos por contenido a mano en cada llamada es
        // lo que esto evita.
        if (this.enabledCipherSuites != null) {
            this.enabledCipherSuitesList = new ArrayList<String>(
                    Arrays.asList(this.enabledCipherSuites));
        }
        if (this.enabledProtocols != null) {
            this.enabledProtocolsList = new ArrayList<String>(
                    Arrays.asList(this.enabledProtocols));
        }

        SSLSocket prueba;
        try {
            prueba = (SSLSocket) fabrica().createSocket();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to check if the cipher suites and protocols to enable are supported", e);
        }
        if (this.enabledCipherSuites != null) {
            prueba.setEnabledCipherSuites(this.enabledCipherSuites);
        }
        if (this.enabledProtocols != null) {
            prueba.setEnabledProtocols(this.enabledProtocols);
        }
    }

    /** Las suites habilitadas, o `null` si son las de siempre. */
    public final String[] getEnabledCipherSuites() {
        return this.enabledCipherSuites == null ? null : this.enabledCipherSuites.clone();
    }

    /** Los protocolos habilitados, o `null` si son los de siempre. */
    public final String[] getEnabledProtocols() {
        return this.enabledProtocols == null ? null : this.enabledProtocols.clone();
    }

    /** Si se le exige certificado al cliente. */
    public final boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /**
     * Un socket de escucha que negocia SSL en cada `accept`.
     *
     * <p>El handshake no pasa aca sino en el `accept`: crear el socket de escucha no habla con
     * nadie.
     *
     * @throws IOException si no se puede escuchar en ese puerto
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new SslServerSocket(port, fabrica(), this.enabledCipherSuites,
                this.enabledProtocols, this.needClientAuth);
    }

    /**
     * Dos fabricas de la misma clase con la misma configuracion.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        return checkParameters((SslRMIServerSocketFactory) obj);
    }

    /** La comparacion de configuracion que usa {@link #equals}. */
    private boolean checkParameters(SslRMIServerSocketFactory that) {
        if (this.needClientAuth != that.needClientAuth) {
            return false;
        }
        if (this.context != that.context) {
            return false;
        }
        if (this.enabledCipherSuites == null ? that.enabledCipherSuites != null
                : !this.enabledCipherSuitesList.equals(that.enabledCipherSuitesList)) {
            return false;
        }
        if (this.enabledProtocols == null ? that.enabledProtocols != null
                : !this.enabledProtocolsList.equals(that.enabledProtocolsList)) {
            return false;
        }
        return true;
    }

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        return this.getClass().hashCode()
                + (this.needClientAuth ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode())
                + (this.enabledCipherSuites == null ? 0 : this.enabledCipherSuitesList.hashCode())
                + (this.enabledProtocols == null ? 0 : this.enabledProtocolsList.hashCode());
    }

    /** La fabrica de sockets: la del contexto si hay, la de siempre si no. */
    private SSLSocketFactory fabrica() {
        return this.context == null ? getDefaultSSLSocketFactory() : this.context.getSocketFactory();
    }

    /** La fabrica SSL de siempre, memorizada; armarla no es gratis. */
    private static synchronized SSLSocketFactory getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return defaultSSLSocketFactory;
    }
}
