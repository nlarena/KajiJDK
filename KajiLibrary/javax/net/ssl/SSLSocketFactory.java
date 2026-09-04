package javax.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * Fabrica de {@link SSLSocket}.
 *
 * <h2>Por que una fabrica y no un constructor</h2>
 *
 * <p>Para que el codigo que abre conexiones no sepa si son seguras. Un metodo que recibe una
 * {@link SocketFactory} y llama a {@code createSocket} sirve igual para TLS que para texto plano, y
 * elegir cual es una decision de configuracion en otro lado. Ese desacople es todo el motivo de que
 * {@code javax.net} exista.
 *
 * <h2>El {@code createSocket} que envuelve otro socket</h2>
 *
 * <p>{@link #createSocket(Socket, String, int, boolean)} no abre nada: toma una conexion ya abierta
 * y le pone TLS encima. Es lo que permite <strong>empezar en claro y despues cifrar</strong>, que es
 * como funcionan {@code STARTTLS} y los proxies HTTP con {@code CONNECT}.
 *
 * <h2>Sin proveedor instalado</h2>
 *
 * <p>{@link #getDefault} no falla: devuelve una fabrica cuyos {@code createSocket} tiran
 * {@link SocketException}. Es exactamente lo que hace el JDK, y la razon es que esta firma no puede
 * declarar excepcion — asi que el error se aplaza hasta el momento en que alguien intente usarla, y
 * ahi si tiene donde salir.
 */
public abstract class SSLSocketFactory extends SocketFactory {

    private static SSLSocketFactory laDefault;

    public SSLSocketFactory() {
    }

    /**
     * La fabrica por omision.
     *
     * <p>Sale del {@link SSLContext} por omision. Sin proveedor de TLS instalado —el caso de esta
     * VM— devuelve una que falla al usarse; ver la nota de la clase.
     */
    public static synchronized SocketFactory getDefault() {
        if (laDefault == null) {
            try {
                laDefault = (SSLSocketFactory) SSLContext.getDefault().getSocketFactory();
            } catch (Exception e) {
                laDefault = new DefaultSSLSocketFactory(e);
            }
        }
        return laDefault;
    }

    /** Las suites habilitadas por omision en lo que fabrique. */
    public abstract String[] getDefaultCipherSuites();

    /** Todas las suites que se podrian habilitar. */
    public abstract String[] getSupportedCipherSuites();

    /**
     * Le pone TLS a una conexion ya abierta.
     *
     * @param s la conexion existente
     * @param host el nombre del par, para verificarlo y para SNI
     * @param autoClose si cerrar {@code s} al cerrar el socket devuelto
     */
    public abstract Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException;

    /**
     * Igual, pero devolviendo primero unos bytes que ya se habian leido.
     *
     * <p>Resuelve un problema de multiplexado: quien mira el primer byte para decidir si la conexion
     * es TLS ya lo saco del flujo, y el handshake necesita verlo. Este metodo lo vuelve a poner
     * adelante.
     */
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose)
            throws IOException {
        throw new UnsupportedOperationException(
                "esta fabrica no sabe reinyectar los bytes ya consumidos");
    }
}
