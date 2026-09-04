package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Un {@link Socket} que cifra.
 *
 * <h2>La promesa, y lo que hay que hacer para que sea cierta</h2>
 *
 * <p>Todo lo de {@code Socket} sigue valiendo: se lee y se escribe igual, y el cifrado pasa abajo.
 * Eso es lo que permite tomar codigo que hablaba en claro y volverlo seguro cambiando quien crea el
 * socket.
 *
 * <p>Con una salvedad que cuesta cara: <strong>por omision no se verifica la identidad del
 * servidor</strong>. Un socket recien creado cifra contra quien sea, incluido un intermediario con
 * un certificado legitimo de otro dominio. Encender esa verificacion es poner
 * {@link SSLParameters#setEndpointIdentificationAlgorithm} en {@code "HTTPS"}. No hacerlo es la
 * forma mas frecuente de tener TLS que no protege de nada.
 *
 * <h2>Cuando pasa el handshake</h2>
 *
 * <p>No al crear el socket: en la primera lectura o escritura, o cuando se lo pida
 * {@link #startHandshake}. Por eso un error de certificado no aparece donde uno lo espera sino en
 * el primer {@code read} — y por eso conviene llamar a {@code startHandshake} explicitamente cuando
 * se quiere fallar temprano.
 */
public abstract class SSLSocket extends Socket {

    /** Sin conectar. */
    protected SSLSocket() {
        super();
    }

    /** Conectado a un host por nombre. */
    protected SSLSocket(String host, int port) throws IOException, UnknownHostException {
        super(host, port);
    }

    /** Conectado a una direccion. */
    protected SSLSocket(InetAddress address, int port) throws IOException {
        super(address, port);
    }

    /** Conectado, ligando ademas una direccion local. */
    protected SSLSocket(String host, int port, InetAddress clientAddress, int clientPort)
            throws IOException, UnknownHostException {
        super(host, port, clientAddress, clientPort);
    }

    /** Igual, con la direccion remota ya resuelta. */
    protected SSLSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort)
            throws IOException {
        super(address, port, clientAddress, clientPort);
    }

    /** Todas las suites que este socket conoce. */
    public abstract String[] getSupportedCipherSuites();

    /** Las habilitadas ahora. */
    public abstract String[] getEnabledCipherSuites();

    /** Fija las suites habilitadas. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** Todos los protocolos que conoce. */
    public abstract String[] getSupportedProtocols();

    /** Los habilitados ahora. */
    public abstract String[] getEnabledProtocols();

    /** Fija los protocolos habilitados. */
    public abstract void setEnabledProtocols(String[] protocols);

    /**
     * La sesion, forzando el handshake si todavia no paso.
     *
     * <p>Bloquea, y si el handshake falla <strong>no tira</strong>: devuelve una sesion invalida con
     * suite {@code SSL_NULL_WITH_NULL_NULL}. Es una firma vieja que no podia declarar excepcion, y
     * la trampa esta en que el error se ve solo si uno mira la suite.
     */
    public abstract SSLSession getSession();

    /** La sesion en negociacion, o {@code null} si no hay handshake en curso. */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException("este socket no expone la sesion en negociacion");
    }

    /** Agrega quien se entere de cada handshake terminado. */
    public abstract void addHandshakeCompletedListener(HandshakeCompletedListener listener);

    /** Saca un oyente. */
    public abstract void removeHandshakeCompletedListener(HandshakeCompletedListener listener);

    /**
     * Fuerza el handshake, o renegocia si ya hubo uno.
     *
     * @throws IOException si el handshake falla — a diferencia de {@link #getSession}, aca el error
     *     si llega como excepcion, que es la razon para llamarlo explicitamente
     */
    public abstract void startHandshake() throws IOException;

    /**
     * Si este socket es el cliente.
     *
     * @throws IllegalArgumentException si el handshake ya empezo
     */
    public abstract void setUseClientMode(boolean mode);

    /** Si es el cliente. */
    public abstract boolean getUseClientMode();

    /** Exige autenticacion de cliente; solo del lado servidor. */
    public abstract void setNeedClientAuth(boolean need);

    /** Si se exige. */
    public abstract boolean getNeedClientAuth();

    /** Pide autenticacion de cliente sin exigirla. */
    public abstract void setWantClientAuth(boolean want);

    /** Si se pide. */
    public abstract boolean getWantClientAuth();

    /** Si se pueden crear sesiones nuevas. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Si se pueden crear sesiones nuevas. */
    public abstract boolean getEnableSessionCreation();

    /** Toda la configuracion junta. */
    public SSLParameters getSSLParameters() {
        SSLParameters p = new SSLParameters();
        p.setCipherSuites(getEnabledCipherSuites());
        p.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            p.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            p.setWantClientAuth(true);
        }
        return p;
    }

    /** Aplica la configuracion; solo lo que no sea {@code null}. */
    public void setSSLParameters(SSLParameters params) {
        String[] s = params.getCipherSuites();
        if (s != null) {
            setEnabledCipherSuites(s);
        }
        s = params.getProtocols();
        if (s != null) {
            setEnabledProtocols(s);
        }
        if (params.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (params.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }

    /** El protocolo de aplicacion acordado por ALPN. */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException("este socket no soporta ALPN");
    }

    /** El que se va acordando durante el handshake. */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException("este socket no soporta ALPN");
    }

    /** Elige el protocolo de aplicacion con una funcion propia. */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLSocket, List<String>, String> selector) {
        throw new UnsupportedOperationException("este socket no soporta ALPN");
    }

    /** El selector puesto, o {@code null}. */
    public BiFunction<SSLSocket, List<String>, String> getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException("este socket no soporta ALPN");
    }
}
