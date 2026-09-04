package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Un {@link ServerSocket} cuyos {@code accept} devuelven {@link SSLSocket}.
 *
 * <h2>Para que sirve configurarlo aca y no en cada socket aceptado</h2>
 *
 * <p>Porque lo que se fija en este objeto son los <strong>valores por omision</strong> de todo lo
 * que acepte de ahi en mas. Un servidor que exige certificado de cliente lo dice una vez, y no en
 * cada conexion — donde ademas seria tarde: la configuracion tiene que estar puesta antes de que
 * empiece el handshake, y el handshake empieza solo.
 *
 * <p>Los mismos setters existen en {@link SSLSocket} para el caso contrario: cambiarle algo a
 * <em>una</em> conexion sin tocar las demas.
 */
public abstract class SSLServerSocket extends ServerSocket {

    /** Sin ligar. */
    protected SSLServerSocket() throws IOException {
        super();
    }

    /** Escuchando en un puerto. */
    protected SSLServerSocket(int port) throws IOException {
        super(port);
    }

    /** Con la cantidad de conexiones en espera. */
    protected SSLServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    /** Ligado ademas a una direccion local concreta. */
    protected SSLServerSocket(int port, int backlog, InetAddress address) throws IOException {
        super(port, backlog, address);
    }

    /** Las suites habilitadas por omision para lo que se acepte. */
    public abstract String[] getEnabledCipherSuites();

    /** Fija las suites habilitadas. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** Todas las suites que se conocen. */
    public abstract String[] getSupportedCipherSuites();

    /** Todos los protocolos que se conocen. */
    public abstract String[] getSupportedProtocols();

    /** Los protocolos habilitados por omision. */
    public abstract String[] getEnabledProtocols();

    /** Fija los protocolos habilitados. */
    public abstract void setEnabledProtocols(String[] protocols);

    /** Exige autenticacion de cliente en lo que se acepte. */
    public abstract void setNeedClientAuth(boolean need);

    /** Si se exige. */
    public abstract boolean getNeedClientAuth();

    /** La pide sin exigirla. */
    public abstract void setWantClientAuth(boolean want);

    /** Si se pide. */
    public abstract boolean getWantClientAuth();

    /**
     * Si los sockets aceptados actuan como cliente.
     *
     * <p>Suena contradictorio y no lo es: en TLS el rol del handshake no tiene por que coincidir con
     * quien abrio la conexion TCP. Hay protocolos donde el que acepta es el cliente TLS.
     */
    public abstract void setUseClientMode(boolean mode);

    /** Si los aceptados actuan como cliente. */
    public abstract boolean getUseClientMode();

    /** Si se pueden crear sesiones nuevas. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Si se pueden crear sesiones nuevas. */
    public abstract boolean getEnableSessionCreation();

    /** Toda la configuracion por omision, junta. */
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
}
